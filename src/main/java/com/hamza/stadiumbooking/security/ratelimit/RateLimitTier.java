package com.hamza.stadiumbooking.security.ratelimit;

public enum RateLimitTier {
    AUTH(5, 15),
    BUSINESS(30, 80),
    INFRASTRUCTURE(100, 150);

    private final int maxAllowed;
    private final int banThreshold;

    RateLimitTier(int maxAllowed, int banThreshold) {
        this.maxAllowed = maxAllowed;
        this.banThreshold = banThreshold;
    }

    public int maxAllowed() {
        return maxAllowed;
    }

    public int banThreshold() {
        return banThreshold;
    }
}
