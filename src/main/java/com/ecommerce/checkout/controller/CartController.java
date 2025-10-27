package com.ecommerce.checkout.controller;

import com.ecommerce.checkout.dto.*;
import com.ecommerce.checkout.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@Tag(name = "Cart Management", description = "APIs for shopping cart operations")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(
            summary = "Get cart",
            description = "Retrieve the current shopping cart for a user or anonymous user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Missing user identification headers"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public CartSnapshotDto getCart(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId
    ) {
        return cartService.getCart(resolveOwner(userId, anonId));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Add item to cart",
            description = "Add a new item to the shopping cart with idempotency support"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or missing headers"),
            @ApiResponse(responseCode = "409", description = "Idempotency key conflict")
    })
    public CartSnapshotDto addItem(
            @Parameter(description = "Idempotency key to prevent duplicate operations", required = true, example = "idem-12345")
            @RequestHeader("Idempotency-Key") String idemKey,

            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Country code for pricing", required = true, example = "US")
            @RequestHeader("X-Country") String country,

            @Parameter(description = "Currency code", required = true, example = "USD")
            @RequestHeader("X-Currency") String currency,

            @Parameter(description = "Item details to add to cart", required = true)
            @Valid @RequestBody AddToCartRequestDto body
    ) {
        return cartService.addItem(resolveOwner(userId, anonId), country, currency, body, idemKey);
    }

    @PatchMapping("/items/{lineId}")
    @Operation(
            summary = "Update item quantity",
            description = "Update the quantity of a specific item in the cart. Setting quantity to 0 removes the item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quantity updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity or missing headers"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public CartSnapshotDto updateLineQuantity(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Line item ID to update", required = true, example = "line-123")
            @PathVariable String lineId,

            @Parameter(description = "Quantity update details", required = true)
            @Valid @RequestBody UpdateLineQuantityRequestDto body
    ) {
        if (body.quantity() != null && body.quantity() == 0) {
            return cartService.removeLine(resolveOwner(userId, anonId), lineId, body.clientCartVersion());
        }
        return cartService.updateQuantity(resolveOwner(userId, anonId), lineId, body.quantity(), body.clientCartVersion());
    }

    @DeleteMapping("/items/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove item from cart",
            description = "Remove a specific item from the shopping cart"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or item not found")
    })
    public void removeLine(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Line item ID to remove", required = true, example = "line-123")
            @PathVariable String lineId,

            @Parameter(description = "Client cart version for optimistic locking", example = "5")
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        cartService.removeLine(resolveOwner(userId, anonId), lineId, clientCartVersion);
    }

    @PostMapping("/coupons")
    @Operation(
            summary = "Apply coupon code",
            description = "Apply a discount coupon to the shopping cart"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon code"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public CartSnapshotDto applyCoupon(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Coupon application details", required = true)
            @Valid @RequestBody ApplyCouponRequestDto body
    ) {
        return cartService.applyCoupon(resolveOwner(userId, anonId), body.couponCode(), body.clientCartVersion());
    }

    @DeleteMapping("/coupons/{code}")
    @Operation(
            summary = "Remove coupon",
            description = "Remove a coupon from the shopping cart"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully"),
            @ApiResponse(responseCode = "404", description = "Cart or coupon not found")
    })
    public CartSnapshotDto removeCoupon(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Coupon code to remove", required = true, example = "SUMMER2024")
            @PathVariable String code,

            @Parameter(description = "Client cart version for optimistic locking", example = "5")
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        return cartService.removeCoupon(resolveOwner(userId, anonId), code, clientCartVersion);
    }

    @PostMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Clear cart",
            description = "Remove all items from the shopping cart"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "404", description = "Cart not found")
    })
    public void clear(
            @Parameter(description = "User ID for authenticated users", example = "user-123")
            @RequestHeader(value = "X-User-Id", required = false) String userId,

            @Parameter(description = "Anonymous ID for guest users", example = "anon-abc123")
            @RequestHeader(value = "X-Anon-Id", required = false) String anonId,

            @Parameter(description = "Client cart version for optimistic locking", example = "5")
            @RequestParam(required = false) Integer clientCartVersion
    ) {
        cartService.clear(resolveOwner(userId, anonId), clientCartVersion);
    }

    @PostMapping("/merge")
    @Operation(
            summary = "Merge carts",
            description = "Merge a guest cart into a user cart when user logs in"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carts merged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid merge request")
    })
    public CartSnapshotDto merge(
            @Parameter(description = "Cart merge details", required = true)
            @Valid @RequestBody MergeCartRequestDto body
    ) {
        return cartService.merge(body.guestAnonId(), body.userId());
    }

    // --- helpers ---
    private String resolveOwner(String userId, String anonId) {
        if (userId != null && !userId.isBlank()) return "user:" + userId;
        if (anonId != null && !anonId.isBlank()) return "anon:" + anonId;
        throw new IllegalArgumentException("Missing X-User-Id or X-Anon-Id header");
    }
}