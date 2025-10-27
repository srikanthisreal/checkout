package com.ecommerce.checkout.service;

import com.ecommerce.checkout.dto.CartSnapshotDto;
import com.ecommerce.checkout.dto.AddToCartRequestDto;

public interface CartService {
    CartSnapshotDto getCart(String ownerKey);

    CartSnapshotDto addItem(String ownerKey, String country, String currency,
                            AddToCartRequestDto req, String idempotencyKey);

    CartSnapshotDto updateQuantity(String ownerKey, String lineId, int newQty, Integer clientCartVersion);

    CartSnapshotDto removeLine(String ownerKey, String lineId, Integer clientCartVersion);

    CartSnapshotDto applyCoupon(String ownerKey, String couponCode, Integer clientCartVersion);

    CartSnapshotDto removeCoupon(String ownerKey, String couponCode, Integer clientCartVersion);

    void clear(String ownerKey, Integer clientCartVersion);

    CartSnapshotDto merge(String guestAnonId, String userId);
}
