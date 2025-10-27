package com.ecommerce.checkout.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document("cart")
@CompoundIndexes({
        // fetch by owner quickly
        @CompoundIndex(name="owner_user_idx", def="{ 'ownerUserId':1 }"),
        @CompoundIndex(name="owner_anon_idx", def="{ 'ownerAnonId':1 }")
})
public class CartDocument {
    @Id
    public String id;

    @Indexed
    public String ownerUserId;      // nullable for guest
    @Indexed
    public String ownerAnonId;      // anonSessionId for guests

    public Integer version;         // optimistic concurrency
    public String currency;         // "EUR"
    public String country;          // "DE"

    public Long subtotalCents;
    public Long promoDiscountCents;
    public Long taxCents;
    public Long shippingCents;      // usually null until checkout
    public Long totalCents;

    public List<Line> lines;

    public List<String> warnings;

    public Instant createdAt;
    public Instant updatedAt;

    public static class Line {
        public String lineId;         // UUID for stable reference
        public String sku;
        public Long productId;
        public Integer qty;

        public Price price;
        public Availability availability;

        public Map<String, String> attributes; // variant attrs
        public Map<String, String> metadata;   // placement/channel/offer/seller

        public static class Price {
            public Long unitPriceCents;
            public Long unitPromoAdjCents;
            public Long unitTaxCents;
            public Long lineBaseCents;
            public Long linePromoAdjCents;
            public Long lineTaxCents;
            public Long lineTotalCents;
        }

        public static class Availability {
            public boolean inStock;
            public boolean backorder;
            public Integer availableQty;
            public String etaText;
        }
    }
}
