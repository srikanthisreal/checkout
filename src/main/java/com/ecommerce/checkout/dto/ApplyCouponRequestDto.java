package com.ecommerce.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to apply coupon code to cart")
public record ApplyCouponRequestDto(
        @Schema(
                description = "Coupon code to apply",
                example = "SUMMER2024",
                maxLength = 64,
                required = true
        )
        @NotBlank @Size(max = 64) String couponCode,

        @Schema(
                description = "Client cart version for optimistic locking",
                example = "5",
                minimum = "0"
        )
        @PositiveOrZero Integer clientCartVersion
) {}