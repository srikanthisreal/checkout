package com.ecommerce.checkout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to merge guest cart into user cart")
public record MergeCartRequestDto(
        @Schema(
                description = "Anonymous ID of the guest cart to merge",
                example = "anon-abc123xyz",
                required = true
        )
        @NotBlank String guestAnonId,     // the guest cart to absorb

        @Schema(
                description = "User ID of the logged-in user to merge into",
                example = "user-12345",
                required = true
        )
        @NotBlank String userId           // logged-in user id
) {}