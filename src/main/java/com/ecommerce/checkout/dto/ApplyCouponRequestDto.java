package com.ecommerce.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ApplyCouponRequestDto(
        @NotBlank @Size(max = 64) String couponCode,
        @PositiveOrZero Integer clientCartVersion
) {
}
