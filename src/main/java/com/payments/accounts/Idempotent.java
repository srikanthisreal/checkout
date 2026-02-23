package com.payments.accounts;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Can only be applied to methods
@Retention(RetentionPolicy.RUNTIME) // Must be available at runtime for AOP to read it
public @interface Idempotent {

    // The HTTP header we expect the client to send
    String headerName() default "Idempotency-Key";

    // How long should Redis remember this request? (Default: 24 hours)
    long ttlSeconds() default 86400;
}
