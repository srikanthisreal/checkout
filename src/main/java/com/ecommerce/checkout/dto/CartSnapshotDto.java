package com.ecommerce.checkout.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CartSnapshotDto(
        String cartId,
        Integer version,              // optimistic locking
        String currency,              // e.g., EUR
        String country,               // e.g., DE
        Long subtotalCents,           // sum of line base amounts (pre-tax, pre-promo) or define your policy
        Long promoDiscountCents,      // total cart-level promo (negative)
        Long taxCents,                // total tax
        Long shippingCents,           // filled during checkout
        Long totalCents,              // subtotal + tax + shipping + line-level promos
        List<Line> lines,
        List<String> warnings,        // e.g., “Only 2 left”, “Price updated”
        Instant updatedAt


) {
    public record Line(
            String lineId,             // stable id for PATCH/DELETE
            String sku,
            Long productId,
            Integer qty,
            PriceBreakdown price,      // final line totals (server-calculated)
            AvailabilityAdvisory availability,
            Map<String, String> attributes,// variant attrs: size/color/brand/etc.
            Map<String, String> metadata// placement/channel/offerId/sellerId …
    ) {}
    public record PriceBreakdown(
            Long unitPriceCents,       // list price (or current price) per unit
            Long unitPromoAdjCents,    // negative per-unit promo (if any)
            Long unitTaxCents,         // per-unit tax if you break it out
            Long lineBaseCents,        // unitPrice * qty
            Long linePromoAdjCents,    // negative, total promo for this line
            Long lineTaxCents,         // total tax for this line
            Long lineTotalCents        // base + promo + tax
    ) {}

    public record AvailabilityAdvisory(
            boolean inStock,
            boolean backorder,         // true if allowed and currently OOS
            Integer availableQty,      // null if unknown
            String etaText             // e.g., “Ships in 2–4 days”
    ) {}
}
