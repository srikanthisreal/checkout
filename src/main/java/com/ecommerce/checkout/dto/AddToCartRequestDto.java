package com.ecommerce.checkout.dto;


import jakarta.validation.constraints.*;

import java.util.Map;

public record AddToCartRequestDto(
        // Identity of what to add (one of sku or productId is required)
        @Size(min = 1, max = 64) String sku,
        @Positive Long productId,
        // Quantity (server will cap/enforce max-per-order)
        @NotNull @Positive @Max(999) Integer quantity,
        // Marketplace / offer context (if multiple sellers per product)
        @Size(max = 64) String offerId,
        // merchant's offer or listing id
        @Size(max = 64) String marketplaceSellerId,
        // seller code if marketplace

        // Variant selection (size/color/etc.) and addons
        @Size(max = 16_384) Map<@Size(max = 64) String, @Size(max = 128) String> variantAttributes,
// e.g. {"size":"M","color":"Black"}
        @Size(max = 64) String bundleId,
        // tie lines that must go together
        @Size(max = 64) String subscriptionPlanId,
        // if "subscribe & save"
        // Pharmacy specifics (optional; validate elsewhere if present)
        @Size(max = 256) String prescriptionToken,
        // eRx/e-prescription token if required
        @Size(max = 64) String prescriptionId,             // internal linkage
        @AssertTrue(message = "Must accept generic substitution for Rx where required") Boolean acceptGenericSubstitution,
        // can be null; policies decide when to require true
        // Fulfillment intent
        @Size(max = 32) String fulfillmentRegion,
// e.g., "DE-NW" or warehouse region code
        @NotNull Boolean acceptBackorder,
// if true, allow adding even if not currently in stock

        // Promotion / loyalty
        @Size(max = 64) String couponCode,
// optional, can also be applied at cart-level later
        @Size(max = 64) String loyaltyId,
        // loyalty / membership reference

        // Gift / personalization
        @NotNull Boolean giftWrap, @Size(max = 500) String giftMessage,

        // Client-side telemetry (for analytics / A/B source attribution)
        @NotNull Placement placement,// PDP, PLP, CART_CROSS_SELL, RECO, SEARCH
        @NotNull Channel channel,                   // WEB, IOS, ANDROID

        // Concurrency & expectations (client cannot set price; this is for drift checks)
        @PositiveOrZero Integer clientCartVersion,
// optimistic concurrency on cart updates
        PriceExpectation priceExpectation// optional min/max bounds or checksum


) {

    public enum Placement {PDP, PLP, CART_CROSS_SELL, RECO, SEARCH, BUY_NOW}

    public enum Channel {WEB, IOS, ANDROID}

    public static record PriceExpectation(
            @PositiveOrZero Long minUnitPriceCents,
            @PositiveOrZero Long maxUnitPriceCents,
            @Size(max = 64) String snapshotId,
            // last pricing snapshot id you showed
            @Size(max = 128) String hash
            // optional integrity hash of prior totals
    ) {
    }

}