package com.ecommerce.checkout.controller;

import com.ecommerce.checkout.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ecommerce.checkout.service.CartService;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartSnapshotDto getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId
    ) {
        return cartService.getCart(resolveOwner(userId, anonId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartSnapshotDto addItem(
            @RequestHeader("Idempotency-Key") String idemKey,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestHeader("X-Country") String country,
            @RequestHeader("X-Currency") String currency,
            @Valid @RequestBody AddToCartRequestDto body
    ) {
        return cartService.addItem(resolveOwner(userId, anonId), country, currency, body, idemKey);
    }

    @PatchMapping("/items/{lineId}")
    public CartSnapshotDto updateLineQuantity(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @PathVariable String lineId,
            @Valid @RequestBody UpdateLineQuantityRequestDto body
    ) {
        if (body.quantity() != null && body.quantity() == 0) {
            return cartService.removeLine(resolveOwner(userId, anonId), lineId, body.clientCartVersion());
        }
        return cartService.updateQuantity(resolveOwner(userId, anonId), lineId, body.quantity(), body.clientCartVersion());
    }

    @DeleteMapping("/items/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLine(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @PathVariable String lineId,
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        cartService.removeLine(resolveOwner(userId, anonId), lineId, clientCartVersion);
    }

    @PostMapping("/coupons")
    public CartSnapshotDto applyCoupon(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @Valid @RequestBody ApplyCouponRequestDto body
    ) {
        return cartService.applyCoupon(resolveOwner(userId, anonId), body.couponCode(), body.clientCartVersion());
    }

    @DeleteMapping("/coupons/{code}")
    public CartSnapshotDto removeCoupon(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @PathVariable String code,
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        return cartService.removeCoupon(resolveOwner(userId, anonId), code, clientCartVersion);
    }

    @PostMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        cartService.clear(resolveOwner(userId, anonId), clientCartVersion);
    }

    @PostMapping("/merge")
    public CartSnapshotDto merge(@Valid @RequestBody MergeCartRequestDto body) {
        return cartService.merge(body.guestAnonId(), body.userId());
    }


    // --- helpers ---
    private String resolveOwner(String userId, String anonId) {
        if (userId != null && !userId.isBlank()) return "user:" + userId;
        if (anonId != null && !anonId.isBlank()) return "anon:" + anonId;
        throw new IllegalArgumentException("Missing X-User-Id or X-Anon-Id header");
    }
}
