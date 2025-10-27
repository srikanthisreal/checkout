package com.ecommerce.checkout.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateLineQuantityRequestDto(
        @NotNull @Min(0) @Max(999) Integer quantity,    // 0 = remove line
        @PositiveOrZero Integer clientCartVersion       // optional optimistic-lock hint
) {}
