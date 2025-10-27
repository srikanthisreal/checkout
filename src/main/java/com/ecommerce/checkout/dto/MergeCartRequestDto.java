package com.ecommerce.checkout.dto;

import jakarta.validation.constraints.NotBlank;

public record MergeCartRequestDto(
        @NotBlank String guestAnonId,     // the guest cart to absorb
        @NotBlank String userId           // logged-in user id
) {}