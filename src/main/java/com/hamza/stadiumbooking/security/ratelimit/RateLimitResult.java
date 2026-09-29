package com.hamza.stadiumbooking.security.ratelimit;

public record RateLimitResult(long count, long retryAfterSeconds, boolean banThresholdExceeded) {
}
