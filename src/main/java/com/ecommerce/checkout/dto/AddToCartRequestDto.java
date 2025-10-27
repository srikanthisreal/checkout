package com.ecommerce.checkout.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Map;

@Schema(description = "Request to add item to shopping cart")
public record AddToCartRequestDto(
        // Identity of what to add (one of sku or productId is required)
        @Schema(
                description = "Stock Keeping Unit identifier",
                example = "SKU-12345-BLACK-M",
                minLength = 1,
                maxLength = 64
        )
        @Size(min = 1, max = 64) String sku,

        @Schema(
                description = "Product identifier",
                example = "123456789",
                minimum = "1"
        )
        @Positive Long productId,

        // Quantity (server will cap/enforce max-per-order)
        @Schema(
                description = "Quantity to add to cart",
                example = "2",
                minimum = "1",
                maximum = "999",
                required = true
        )
        @NotNull @Positive @Max(999) Integer quantity,

        // Marketplace / offer context (if multiple sellers per product)
        @Schema(
                description = "Offer identifier for marketplace items",
                example = "OFFER-67890",
                maxLength = 64
        )
        @Size(max = 64) String offerId,

        @Schema(
                description = "Marketplace seller identifier",
                example = "SELLER-ABC123",
                maxLength = 64
        )
        @Size(max = 64) String marketplaceSellerId,
        // seller code if marketplace

        // Variant selection (size/color/etc.) and addons
        @Schema(
                description = "Product variant attributes (size, color, etc.)",
                example = "{\"size\": \"M\", \"color\": \"Black\", \"material\": \"Cotton\"}",
                maxLength = 16384
        )
        @Size(max = 16_384) Map<@Size(max = 64) String, @Size(max = 128) String> variantAttributes,
// e.g. {"size":"M","color":"Black"}

        @Schema(
                description = "Bundle identifier for grouped products",
                example = "BUNDLE-SUMMER-2024",
                maxLength = 64
        )
        @Size(max = 64) String bundleId,
        // tie lines that must go together

        @Schema(
                description = "Subscription plan identifier for subscribe & save",
                example = "SUB-MONTHLY-001",
                maxLength = 64
        )
        @Size(max = 64) String subscriptionPlanId,
        // if "subscribe & save"

        // Pharmacy specifics (optional; validate elsewhere if present)
        @Schema(
                description = "E-prescription token for pharmacy items",
                example = "RX-TOKEN-ABC123XYZ",
                maxLength = 256
        )
        @Size(max = 256) String prescriptionToken,
        // eRx/e-prescription token if required

        @Schema(
                description = "Internal prescription identifier",
                example = "PRESCRIPTION-001",
                maxLength = 64
        )
        @Size(max = 64) String prescriptionId,             // internal linkage

        @Schema(
                description = "Whether to accept generic substitution for prescriptions",
                example = "true"
        )
        @AssertTrue(message = "Must accept generic substitution for Rx where required") Boolean acceptGenericSubstitution,
        // can be null; policies decide when to require true

        // Fulfillment intent
        @Schema(
                description = "Fulfillment region or warehouse code",
                example = "DE-NW",
                maxLength = 32
        )
        @Size(max = 32) String fulfillmentRegion,
// e.g., "DE-NW" or warehouse region code

        @Schema(
                description = "Whether to allow backorder if item is out of stock",
                example = "false",
                required = true
        )
        @NotNull Boolean acceptBackorder,
// if true, allow adding even if not currently in stock

        // Promotion / loyalty
        @Schema(
                description = "Coupon code to apply with this item",
                example = "SUMMER20",
                maxLength = 64
        )
        @Size(max = 64) String couponCode,
// optional, can also be applied at cart-level later

        @Schema(
                description = "Loyalty program membership identifier",
                example = "LOYALTY-12345",
                maxLength = 64
        )
        @Size(max = 64) String loyaltyId,
        // loyalty / membership reference

        // Gift / personalization
        @Schema(
                description = "Whether to apply gift wrapping",
                example = "true",
                required = true
        )
        @NotNull Boolean giftWrap,

        @Schema(
                description = "Gift message for the item",
                example = "Happy Birthday!",
                maxLength = 500
        )
        @Size(max = 500) String giftMessage,

        // Client-side telemetry (for analytics / A/B source attribution)
        @Schema(
                description = "Placement context where add-to-cart was initiated",
                example = "PDP",
                required = true
        )
        @NotNull Placement placement,// PDP, PLP, CART_CROSS_SELL, RECO, SEARCH

        @Schema(
                description = "Channel where the request originated",
                example = "WEB",
                required = true
        )
        @NotNull Channel channel,                   // WEB, IOS, ANDROID

        // Concurrency & expectations (client cannot set price; this is for drift checks)
        @Schema(
                description = "Client cart version for optimistic concurrency control",
                example = "5",
                minimum = "0"
        )
        @PositiveOrZero Integer clientCartVersion,
// optimistic concurrency on cart updates

        @Schema(
                description = "Price expectation for validation"
        )
        PriceExpectation priceExpectation// optional min/max bounds or checksum
) {

    @Schema(description = "Placement context for add-to-cart action")
    public enum Placement {
        @Schema(description = "Product Detail Page") PDP,
        @Schema(description = "Product Listing Page") PLP,
        @Schema(description = "Cart Cross-sell") CART_CROSS_SELL,
        @Schema(description = "Recommendation") RECO,
        @Schema(description = "Search results") SEARCH,
        @Schema(description = "Buy Now button") BUY_NOW
    }

    @Schema(description = "Channel where the request originated")
    public enum Channel {
        @Schema(description = "Web browser") WEB,
        @Schema(description = "iOS mobile app") IOS,
        @Schema(description = "Android mobile app") ANDROID
    }

    @Schema(description = "Price expectation for validation")
    public static record PriceExpectation(
            @Schema(
                    description = "Minimum expected unit price in cents",
                    example = "1999",
                    minimum = "0"
            )
            @PositiveOrZero Long minUnitPriceCents,

            @Schema(
                    description = "Maximum expected unit price in cents",
                    example = "2499",
                    minimum = "0"
            )
            @PositiveOrZero Long maxUnitPriceCents,

            @Schema(
                    description = "Pricing snapshot identifier",
                    example = "PRICE-SNAP-12345",
                    maxLength = 64
            )
            @Size(max = 64) String snapshotId,
            // last pricing snapshot id you showed

            @Schema(
                    description = "Integrity hash of prior pricing data",
                    example = "a1b2c3d4e5f6",
                    maxLength = 128
            )
            @Size(max = 128) String hash
            // optional integrity hash of prior totals
    ) {
    }
}