package com.ecommerce.checkout.exception;

import com.ecommerce.checkout.util.CheckoutConstants;

public class VersionMismatchException extends AppException {
  public final String cartId;
  public final Integer currentVersion;
  public VersionMismatchException(String message, String cartId, Integer currentVersion) {
    super(CheckoutConstants.VERSION_MISMATCH, message);
    this.cartId = cartId; this.currentVersion = currentVersion;
  }
}