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

        // --- Normalize request and compute hash ---
        // Use only fields that define the operation (don’t include clientCartVersion)
        var canonical = new StringBuilder()
                .append("sku=").append(Objects.toString(req.sku(),""))
                .append("|pid=").append(Objects.toString(req.productId(),""))
                .append("|qty=").append(req.quantity())
                .append("|attrs=").append(new java.util.TreeMap<>(Optional.ofNullable(req.variantAttributes()).orElse(Map.of())))
                .append("|seller=").append(Objects.toString(req.marketplaceSellerId(),""))
                .append("|offer=").append(Objects.toString(req.offerId(),""))
                .append("|backorder=").append(Objects.toString(req.acceptBackorder(), ""))
                .toString();
        var requestHash = HashUtil.sha256(canonical);

        // --- Idempotency check / replay ---
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idems.findById(idempotencyKey);
            if (existing.isPresent()) {
                var entry = existing.get();

                // 1) Key reused with a different body? -> 409
                if (entry.requestHash != null && !entry.requestHash.equals(requestHash)) {
                    throw new IdempotencyConflictException(
                            "Idempotency-Key reused with a different request body");
                }

                // 2) If you cached a previous response, replay it (optional)
                if (entry.responseCache != null && !entry.responseCache.isBlank()) {
                    try {
                        return new com.fasterxml.jackson.databind.ObjectMapper()
                                .readValue(entry.responseCache, CartSnapshotDto.class);
                    } catch (Exception ignore) {
                        // fall back to return current state
                    }
                }

                // 3) Fallback: return current snapshot (idempotent effect)
                return CartMapper.toDto(loadOrCreate(ownerKey, currency, country));
            }
        }

        // --- Proceed with normal add logic (unchanged) ---
        var cart = loadOrCreate(ownerKey, currency, country);

        if (req.clientCartVersion() != null && !req.clientCartVersion().equals(cart.version)) {
            throw new VersionMismatchException("Cart version mismatch; please refresh",
                    cart.id, cart.version);
        }

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
            line.qty = Math.min(999, line.qty + Math.max(1, req.quantity()));
        }

        reprice(cart);
        availability(cart, Boolean.TRUE.equals(req.acceptBackorder()));

        bump(cart);
        carts.save(cart);

        var dto = CartMapper.toDto(cart);

        // --- Persist idempotency entry (with request hash + cached response) ---
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String responseJson = null;
            try { responseJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dto); }
            catch (Exception ignore) {}
            var entry = new IdempotencyEntry(idempotencyKey, ownerKey, requestHash);
            entry.responseCache = responseJson;
            idems.save(entry);
        }

        return dto;
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
        cart.taxCents = Math.round(nz(cart.subtotalCents + nz(cart.promoDiscountCents)) * 0.19); // example vat added - 19% VAT
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
