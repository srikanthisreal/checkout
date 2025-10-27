package com.ecommerce.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request to update item quantity in cart")
public record UpdateLineQuantityRequestDto(
        @Schema(
                description = "New quantity for the item (0 = remove from cart)",
                example = "3",
                minimum = "0",
                maximum = "999",
                required = true
        )
        @NotNull @Min(0) @Max(999) Integer quantity,    // 0 = remove line

        @Schema(
                description = "Client cart version for optimistic locking",
                example = "5",
                minimum = "0"
        )
        @PositiveOrZero Integer clientCartVersion       // optional optimistic-lock hint
) {}