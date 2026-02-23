package com.payments.accounts;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    // Prefix to separate idempotency keys from other Redis data
    private static final String REDIS_PREFIX = "idemp:req:";

    public IdempotencyAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String idempotencyKey = attributes.getRequest().getHeader(idempotent.headerName());

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key is missing");
        }

        String redisKey = REDIS_PREFIX + idempotencyKey;

        // 1. The Distributed Lock Check (SETNX)
        // We try to save a "PENDING" status. If it returns false, another thread is already processing this!
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                redisKey, "PENDING", Duration.ofSeconds(idempotent.ttlSeconds()));

        if (Boolean.FALSE.equals(acquired)) {
            // It already exists! Let's check if it's finished or still processing.
            Object cachedResponse = redisTemplate.opsForValue().get(redisKey);
            if ("PENDING".equals(cachedResponse)) {
                // Return a 409 Conflict or 429 Too Many Requests. The client is double-clicking too fast.
                throw new ConcurrentRequestException("Request is currently being processed.");
            }
            // Return the cached successful response
            return cachedResponse;
        }

        try {
            // 2. We got the lock! Execute the actual Controller method.
            Object actualResponse = joinPoint.proceed();

            // 3. Save the successful response over the "PENDING" marker
            if (actualResponse instanceof ResponseEntity<?> responseEntity && responseEntity.getStatusCode().is2xxSuccessful()) {
                redisTemplate.opsForValue().set(redisKey, actualResponse, Duration.ofSeconds(idempotent.ttlSeconds()));
            } else {
                // If it failed (e.g. 400 Bad Request), delete the key so the user can fix the typo and try again
                redisTemplate.delete(redisKey);
            }
            return actualResponse;

        } catch (Exception e) {
            // Clean up the lock if the server crashed or threw an exception
            redisTemplate.delete(redisKey);
            throw e;
        }
    }
}