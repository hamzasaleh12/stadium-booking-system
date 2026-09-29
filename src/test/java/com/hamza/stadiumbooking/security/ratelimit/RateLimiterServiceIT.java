package com.hamza.stadiumbooking.security.ratelimit;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.StructuredTaskScope;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceIT extends AbstractIntegrationTest {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Test
    void checkLimit_ShouldIncrementDistributedCounter() {
        RateLimitResult first = rateLimiterService.checkLimit(
                "rate:ip:test:auth", 5, 15, Duration.ofMinutes(1));
        RateLimitResult second = rateLimiterService.checkLimit(
                "rate:ip:test:auth", 5, 15, Duration.ofMinutes(1));

        assertThat(first.count()).isEqualTo(1);
        assertThat(second.count()).isEqualTo(2);
    }

    @Test
    void checkLimit_ShouldAtomicallyCountConcurrentRequests() throws Exception {
        int numberOfRequests = 20;
        CountDownLatch startGun = new CountDownLatch(1);

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (int index = 0; index < numberOfRequests; index++) {
                scope.fork(() -> {
                    startGun.await();
                    return rateLimiterService.checkLimit(
                            "rate:ip:concurrent:auth", 100, 200, Duration.ofMinutes(1));
                });
            }

            startGun.countDown();
            scope.join();
            scope.throwIfFailed();
        }

        RateLimitResult result = rateLimiterService.checkLimit(
                "rate:ip:concurrent:auth", 100, 200, Duration.ofMinutes(1));
        assertThat(result.count()).isEqualTo(21);
    }
}
