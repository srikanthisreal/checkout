package com.ecommerce.checkout.service.impl;


import com.ecommerce.checkout.dto.AddToCartRequestDto;
import com.ecommerce.checkout.dto.CartSnapshotDto;
import com.ecommerce.checkout.util.CartMapper;
import com.ecommerce.checkout.model.CartDocument;
import com.ecommerce.checkout.model.IdempotencyEntry;
import com.ecommerce.checkout.repository.CartRepository;
import com.ecommerce.checkout.repository.IdempotencyRepository;
import com.ecommerce.checkout.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository carts;
    private final IdempotencyRepository idems;

    public CartServiceImpl(CartRepository carts, IdempotencyRepository idems) {
        this.carts = carts;
        this.idems = idems;
    }


    @Override
    public CartSnapshotDto getCart(String ownerKey) {
        var cart = loadOrCreate(ownerKey, "EUR", "DE");
        return CartMapper.toDto(cart);
    }

    @Override
    public CartSnapshotDto addItem(String ownerKey, String country,
                                   String currency, AddToCartRequestDto req,
                                   String idempotencyKey) {

        // Idempotency (best-effort; for demo we only check presence)
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (idems.findById(idempotencyKey).isPresent()) {
                // return current snapshot for owner
                return CartMapper.toDto(loadOrCreate(ownerKey, currency, country));
            }
        }

        var cart = loadOrCreate(ownerKey, currency, country);

        // optimistic concurrency (if client provided a version)
        if (req.clientCartVersion() != null && !req.clientCartVersion().equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }

        // find or add line
        var line = findLine(cart, req.sku(), req.productId(), req.variantAttributes());
        if (line == null) {
            line = new CartDocument.Line();
            line.lineId = UUID.randomUUID().toString();
            line.sku = req.sku();
            line.productId = req.productId();
            line.qty = Math.max(1, req.quantity());
            line.attributes = safeMap(req.variantAttributes());
            line.metadata = lineMetadata(req);
            line.price = new CartDocument.Line.Price();
            line.availability = new CartDocument.Line.Availability();
            cart.lines = cart.lines == null ? new ArrayList<>() : cart.lines;
            cart.lines.add(line);
        } else {
            line.qty = Math.min(999, line.qty + Math.max(1, req.quantity())); // cap
        }

        // recompute price & availability (stubs you’ll replace with real services)
        reprice(cart);
        availability(cart, req.acceptBackorder() != null && req.acceptBackorder());

        // bump version & timestamps
        cart.version = (cart.version == null ? 0 : cart.version) + 1;
        cart.updatedAt = Instant.now();
        carts.save(cart);

        // persist idempotency key after success
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var entry = new IdempotencyEntry();
            entry.key = idempotencyKey;
            entry.createdAtEpoch = Instant.now().getEpochSecond();
            idems.save(entry);
        }

        return CartMapper.toDto(cart);
    }

    @Override
    public CartSnapshotDto updateQuantity(String ownerKey, String lineId, int newQty, Integer clientCartVersion) {
        var cart = mustLoad(ownerKey);
        if (clientCartVersion != null && !clientCartVersion.equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }
        var line = mustFindLine(cart, lineId);
        if (newQty <= 0) {
            cart.lines.remove(line);
        } else {
            line.qty = Math.min(999, newQty);
        }
        reprice(cart);
        availability(cart, false);
        bump(cart);
        carts.save(cart);
        return CartMapper.toDto(cart);
    }

    @Override
    public CartSnapshotDto removeLine(String ownerKey, String lineId, Integer clientCartVersion) {
        var cart = mustLoad(ownerKey);
        if (clientCartVersion != null && !clientCartVersion.equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }
        var line = mustFindLine(cart, lineId);
        cart.lines.remove(line);
        reprice(cart);
        availability(cart, false);
        bump(cart);
        carts.save(cart);
        return CartMapper.toDto(cart);
    }

    @Override
    public CartSnapshotDto applyCoupon(String ownerKey, String couponCode, Integer clientCartVersion) {
        var cart = mustLoad(ownerKey);
        if (clientCartVersion != null && !clientCartVersion.equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }
        // TODO: validate coupon rules; for demo we apply -5%
        var subtotal = nz(cart.subtotalCents);
        cart.promoDiscountCents = Math.round(subtotal * -0.05);
        repriceTotals(cart);
        bump(cart);
        carts.save(cart);
        return CartMapper.toDto(cart);
    }

    @Override
    public CartSnapshotDto removeCoupon(String ownerKey, String couponCode, Integer clientCartVersion) {
        var cart = mustLoad(ownerKey);
        if (clientCartVersion != null && !clientCartVersion.equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }
        cart.promoDiscountCents = 0L;
        repriceTotals(cart);
        bump(cart);
        carts.save(cart);
        return CartMapper.toDto(cart);
    }

    @Override
    public void clear(String ownerKey, Integer clientCartVersion) {
        var cart = mustLoad(ownerKey);
        if (clientCartVersion != null && !clientCartVersion.equals(cart.version)) {
            throw conflict("Cart version mismatch; please refresh", cart.id, cart.version);
        }
        cart.lines = new ArrayList<>();
        reprice(cart);
        bump(cart);
        carts.save(cart);
    }

    @Override
    public CartSnapshotDto merge(String guestAnonId, String userId) {
        var guest = carts.findByOwnerAnonId(guestAnonId).orElse(null);
        var user  = carts.findByOwnerUserId(userId).orElseGet(() -> {
            var c = new CartDocument();
            c.id = UUID.randomUUID().toString();
            c.ownerUserId = userId;
            c.currency = "EUR";
            c.country = "DE";
            c.version = 0;
            c.lines = new ArrayList<>();
            c.createdAt = c.updatedAt = Instant.now();
            return c;
        });

        if (guest != null && guest.lines != null) {
            for (var gl : guest.lines) {
                var ul = findLine(user, gl.sku, gl.productId, gl.attributes);
                if (ul == null) {
                    if (user.lines == null) user.lines = new ArrayList<>();
                    user.lines.add(gl);
                } else {
                    ul.qty = Math.min(999, ul.qty + gl.qty);
                }
            }
            carts.delete(guest);
        }

        reprice(user);
        bump(user);
        carts.save(user);
        return CartMapper.toDto(user);
    }

    // --- helpers ---

    private CartDocument loadOrCreate(String ownerKey, String currency, String country) {
        if (ownerKey.startsWith("user:")) {
            var userId = ownerKey.substring(5);
            return carts.findByOwnerUserId(userId).orElseGet(() -> newCartForUser(userId, currency, country));
        } else if (ownerKey.startsWith("anon:")) {
            var anon = ownerKey.substring(5);
            return carts.findByOwnerAnonId(anon).orElseGet(() -> newCartForAnon(anon, currency, country));
        }
        throw new IllegalArgumentException("Bad owner key");
    }

    private CartDocument mustLoad(String ownerKey) {
        if (ownerKey.startsWith("user:")) {
            return carts.findByOwnerUserId(ownerKey.substring(5))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cart not found"));
        } else {
            return carts.findByOwnerAnonId(ownerKey.substring(5))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cart not found"));
        }
    }

    private CartDocument newCartForUser(String userId, String currency, String country) {
        var c = baseNew(currency, country);
        c.ownerUserId = userId;
        return carts.save(c);
    }

    private CartDocument newCartForAnon(String anonId, String currency, String country) {
        var c = baseNew(currency, country);
        c.ownerAnonId = anonId;
        return carts.save(c);
    }

    private CartDocument baseNew(String currency, String country) {
        var c = new CartDocument();
        c.id = UUID.randomUUID().toString();
        c.currency = currency;
        c.country = country;
        c.version = 0;
        c.lines = new ArrayList<>();
        c.subtotalCents = 0L;
        c.promoDiscountCents = 0L;
        c.taxCents = 0L;
        c.shippingCents = 0L;
        c.totalCents = 0L;
        c.createdAt = c.updatedAt = Instant.now();
        return c;
    }

    private CartDocument.Line mustFindLine(CartDocument cart, String lineId) {
        return cart.lines.stream()
                .filter(l -> l.lineId.equals(lineId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "line not found"));
    }

    private CartDocument.Line findLine(CartDocument cart, String sku, Long pid, Map<String, String> attrs) {
        if (cart.lines == null) return null;
        return cart.lines.stream()
                .filter(l ->
                        Objects.equals(l.sku, sku) &&
                                Objects.equals(l.productId, pid) &&
                                Objects.equals(nullSafe(l.attributes), nullSafe(attrs))
                ).findFirst().orElse(null);
    }

    private Map<String, String> safeMap(Map<String, String> m) {
        return (m == null) ? new HashMap<>() : new HashMap<>(m);
    }
    private Map<String, String> nullSafe(Map<String, String> m) {
        return (m == null) ? Collections.emptyMap() : m;
    }

    private void reprice(CartDocument cart) {
        // TODO wire real pricing; for demo assume unit price 999 per line
        long subtotal = 0;
        if (cart.lines != null) {
            for (var l : cart.lines) {
                var price = (l.price == null) ? (l.price = new CartDocument.Line.Price()) : l.price;
                price.unitPriceCents = (price.unitPriceCents == null) ? 999L : price.unitPriceCents;
                price.unitPromoAdjCents = (price.unitPromoAdjCents == null) ? 0L : price.unitPromoAdjCents;
                price.unitTaxCents = 0L;
                price.lineBaseCents = price.unitPriceCents * l.qty;
                price.linePromoAdjCents = price.unitPromoAdjCents * l.qty;
                price.lineTaxCents = 0L;
                price.lineTotalCents = price.lineBaseCents + price.linePromoAdjCents + price.lineTaxCents;
                subtotal += price.lineBaseCents;
            }
        }
        cart.subtotalCents = subtotal;
        repriceTotals(cart);
    }

    private void repriceTotals(CartDocument cart) {
        cart.taxCents = Math.round(nz(cart.subtotalCents + nz(cart.promoDiscountCents)) * 0.19); // fake 19% VAT
        cart.totalCents = nz(cart.subtotalCents) + nz(cart.promoDiscountCents) + nz(cart.taxCents);
    }

    private void availability(CartDocument cart, boolean acceptBackorder) {
        if (cart.lines == null) return;
        for (var l : cart.lines) {
            var a = (l.availability == null) ? (l.availability = new CartDocument.Line.Availability()) : l.availability;
            // TODO call availability read model; for demo mark in stock
            a.inStock = true;
            a.backorder = !a.inStock && acceptBackorder;
            a.availableQty = 100;
            a.etaText = a.inStock ? "Ships in 24–48h" : "Ships in 2–4 days";
        }
    }

    private void bump(CartDocument cart) {
        cart.version = (cart.version == null ? 0 : cart.version) + 1;
        cart.updatedAt = Instant.now();
    }

    private ResponseStatusException conflict(String msg, String cartId, Integer currentVersion) {
        return new ResponseStatusException(HttpStatus.CONFLICT, msg + " [cart=" + cartId + ", currentVersion=" + currentVersion + "]");
    }

    private long nz(Long v) { return v == null ? 0L : v; }

    private Map<String, String> lineMetadata(AddToCartRequestDto req) {
        var m = new HashMap<String, String>();
        if (req.placement() != null) m.put("placement", req.placement().name());
        if (req.channel() != null)   m.put("channel", req.channel().name());
        if (req.offerId() != null)   m.put("offerId", req.offerId());
        if (req.marketplaceSellerId()!=null) m.put("sellerId", req.marketplaceSellerId());
        return m;
    }

}
