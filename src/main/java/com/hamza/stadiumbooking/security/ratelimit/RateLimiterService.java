package com.hamza.stadiumbooking.security.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisCallback;
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

    @EventListener(ApplicationReadyEvent.class)
    public void logRedisConnectivity() {
        try {
            String response = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            log.info("RATE_LIMIT_REDIS_CONNECTED status=success response={}", response);
        } catch (RuntimeException exception) {
            log.error("RATE_LIMIT_REDIS_CONNECTED status=failed mode=fail-open error={}",
                    exception.getMessage(), exception);
        }
    }

    public boolean isBanned(String ip) {
        try {
            boolean banned = Boolean.TRUE.equals(redisTemplate.hasKey(banKey(ip)));
            if (banned) {
                log.warn("RATE_LIMIT_BAN_CHECK result=banned ip={}", ip);
            }
            return banned;
        } catch (RuntimeException exception) {
            log.error("Redis ban lookup failed; rate limiting is degraded and the request is being allowed: {}",
                    exception.getMessage(), exception);
            return false;
        }
    }

    public void banIp(String ip, Duration duration) {
        try {
            Boolean created = redisTemplate.opsForValue().setIfAbsent(banKey(ip), "1", duration);
            if (Boolean.TRUE.equals(created)) {
                log.warn("RATE_LIMIT_IP_BANNED ip={} durationSeconds={}", ip, duration.toSeconds());
            } else {
                log.warn("RATE_LIMIT_IP_BAN_ALREADY_PRESENT ip={} durationSeconds={}",
                        ip, duration.toSeconds());
            }
        } catch (RuntimeException exception) {
            log.error("Redis ban write failed; rate limiting is degraded: {}",
                    exception.getMessage(), exception);
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
            boolean banThresholdExceeded = currentCount > banThreshold;
            log.info("RATE_LIMIT_CHECK key={} count={} maxAllowed={} banThreshold={} retryAfterSeconds={} banThresholdExceeded={}",
                    key, currentCount, maxAllowed, banThreshold, retryAfter, banThresholdExceeded);
            return new RateLimitResult(currentCount, retryAfter, banThresholdExceeded);
        } catch (RuntimeException exception) {
            log.error("Redis rate-limit check failed; rate limiting is degraded and the request is being allowed: {}",
                    exception.getMessage(), exception);
            return new RateLimitResult(0, window.toSeconds(), false);
        }
    }

    private String banKey(String ip) {
        return PREFIX + "ban:ip:" + ip;
    }
}
