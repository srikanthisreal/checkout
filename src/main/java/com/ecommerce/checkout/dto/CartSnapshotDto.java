package com.ecommerce.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Shopping cart snapshot with complete pricing and item details")
public record CartSnapshotDto(
        @Schema(description = "Unique cart identifier", example = "c-12345")
        String cartId,

        @Schema(description = "Optimistic locking version for concurrency control", example = "5")
        Integer version,              // optimistic locking

        @Schema(description = "Currency code for all monetary amounts", example = "EUR")
        String currency,              // e.g., EUR

        @Schema(description = "Country code for pricing and tax calculation", example = "DE")
        String country,               // e.g., DE

        @Schema(description = "Sum of line base amounts before tax and promotions (in cents)", example = "19999")
        Long subtotalCents,           // sum of line base amounts (pre-tax, pre-promo) or define your policy

        @Schema(description = "Total cart-level promotional discount (negative amount in cents)", example = "-2000")
        Long promoDiscountCents,      // total cart-level promo (negative)

        @Schema(description = "Total tax amount for the cart (in cents)", example = "3800")
        Long taxCents,                // total tax

        @Schema(description = "Shipping cost (in cents)", example = "499")
        Long shippingCents,           // filled during checkout

        @Schema(description = "Final total amount including all adjustments (in cents)", example = "22298")
        Long totalCents,              // subtotal + tax + shipping + line-level promos

        @Schema(description = "List of items in the cart")
        List<Line> lines,

        @Schema(description = "Warning messages for the cart", example = "[\"Only 2 items left in stock\", \"Price has been updated\"]")
        List<String> warnings,        // e.g., "Only 2 left", "Price updated"

        @Schema(description = "Last update timestamp of the cart", example = "2024-01-15T10:30:00Z")
        Instant updatedAt
) {

    @Schema(description = "Individual line item in the shopping cart")
    public record Line(
            @Schema(description = "Stable identifier for PATCH/DELETE operations", example = "line-67890")
            String lineId,             // stable id for PATCH/DELETE

            @Schema(description = "Stock Keeping Unit", example = "SKU-12345-BLACK-M")
            String sku,

            @Schema(description = "Product identifier", example = "123456789")
            Long productId,

            @Schema(description = "Quantity of this item in cart", example = "2")
            Integer qty,

            @Schema(description = "Detailed price breakdown for this line item")
            PriceBreakdown price,      // final line totals (server-calculated)

            @Schema(description = "Availability information for this item")
            AvailabilityAdvisory availability,

            @Schema(
                    description = "Variant attributes like size, color, brand, etc.",
                    example = "{\"size\": \"M\", \"color\": \"Black\", \"brand\": \"Nike\"}"
            )
            Map<String, String> attributes,// variant attrs: size/color/brand/etc.

            @Schema(
                    description = "Additional metadata like placement, channel, offer details",
                    example = "{\"placement\": \"PDP\", \"channel\": \"WEB\", \"sellerId\": \"SELLER-123\"}"
            )
            Map<String, String> metadata// placement/channel/offerId/sellerId …
    ) {}

    @Schema(description = "Detailed price breakdown for a line item")
    public record PriceBreakdown(
            @Schema(description = "List price per unit (in cents)", example = "9999")
            Long unitPriceCents,       // list price (or current price) per unit

            @Schema(description = "Per-unit promotional adjustment (negative amount in cents)", example = "-1000")
            Long unitPromoAdjCents,    // negative per-unit promo (if any)

            @Schema(description = "Per-unit tax amount (in cents)", example = "1900")
            Long unitTaxCents,         // per-unit tax if you break it out

            @Schema(description = "Base amount for the line (unitPrice × quantity in cents)", example = "19998")
            Long lineBaseCents,        // unitPrice * qty

            @Schema(description = "Total promotional adjustment for this line (negative amount in cents)", example = "-2000")
            Long linePromoAdjCents,    // negative, total promo for this line

            @Schema(description = "Total tax for this line (in cents)", example = "3800")
            Long lineTaxCents,         // total tax for this line

            @Schema(description = "Final total for this line including all adjustments (in cents)", example = "21798")
            Long lineTotalCents        // base + promo + tax
    ) {}

    @Schema(description = "Availability information and advisory for an item")
    public record AvailabilityAdvisory(
            @Schema(description = "Whether the item is currently in stock", example = "true")
            boolean inStock,

            @Schema(description = "Whether backorder is allowed and item is out of stock", example = "false")
            boolean backorder,         // true if allowed and currently OOS

            @Schema(description = "Available quantity (null if unknown)", example = "5")
            Integer availableQty,      // null if unknown

            @Schema(description = "Estimated shipping/delivery time", example = "Ships in 2-4 days")
            String etaText             // e.g., "Ships in 2–4 days"
    ) {}
}