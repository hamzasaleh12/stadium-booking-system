package com.hamza.stadiumbooking.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {
    private static final String PREFIX = "stadium:security:v1:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public boolean isBanned(String ip) {
        try {
            return redisTemplate.hasKey(banKey(ip));
        } catch (RuntimeException exception) {
            log.warn("Redis ban lookup failed; allowing request: {}", exception.getMessage());
            return false;
        }
    }

    public void banIp(String ip, Duration duration) {
        try {
            redisTemplate.opsForValue().setIfAbsent(banKey(ip), "1", duration);
        } catch (RuntimeException exception) {
            log.warn("Redis ban write failed; allowing request: {}", exception.getMessage());
        }
    }

    public RateLimitResult checkLimit(String key, int maxAllowed, int banThreshold, Duration window) {
        try {
            String redisKey = PREFIX + key;
            Long count = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(redisKey),
                    Long.toString(window.toSeconds())
            );
            long currentCount = count == null ? 0L : count;
            Long ttl = redisTemplate.getExpire(redisKey);
            long retryAfter = ttl == null || ttl < 1 ? window.toSeconds() : ttl;
            return new RateLimitResult(currentCount, retryAfter, currentCount > banThreshold);
        } catch (RuntimeException exception) {
            log.warn("Redis rate-limit check failed; allowing request: {}", exception.getMessage());
            return new RateLimitResult(0, window.toSeconds(), false);
        }
    }

    private String banKey(String ip) {
        return PREFIX + "ban:ip:" + ip;
    }
}
