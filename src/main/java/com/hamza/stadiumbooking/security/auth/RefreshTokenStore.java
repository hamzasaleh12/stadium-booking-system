package com.hamza.stadiumbooking.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenStore {
    private static final String KEY_PREFIX = "auth/v1:";
    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if current == ARGV[1] then
                redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3])
                return 1
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public void replace(UUID userId, String refreshToken, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key(userId), hash(refreshToken), ttl);
            log.debug("AUTH_REDIS_SESSION_REPLACED userId={} ttlSeconds={}", userId, ttl.toSeconds());
        } catch (RuntimeException exception) {
            log.error("AUTH_REDIS_SESSION_REPLACE_FAILED userId={} action=fail-closed", userId, exception);
            throw exception;
        }
    }

    public boolean rotate(UUID userId, String presentedToken, String replacementToken, Duration ttl) {
        try {
            Long result = redisTemplate.execute(
                    ROTATE_SCRIPT,
                    List.of(key(userId)),
                    hash(presentedToken),
                    hash(replacementToken),
                    Long.toString(ttl.toSeconds())
            );
            boolean rotated = Long.valueOf(1L).equals(result);
            log.debug("AUTH_REDIS_SESSION_ROTATED userId={} result={} ttlSeconds={}",
                    userId, rotated, ttl.toSeconds());
            return rotated;
        } catch (RuntimeException exception) {
            log.error("AUTH_REDIS_SESSION_ROTATE_FAILED userId={} action=fail-closed", userId, exception);
            throw exception;
        }
    }

    public void delete(UUID userId) {
        try {
            redisTemplate.delete(key(userId));
            log.debug("AUTH_REDIS_SESSION_DELETED userId={}", userId);
        } catch (RuntimeException exception) {
            log.error("AUTH_REDIS_SESSION_DELETE_FAILED userId={}", userId, exception);
            throw exception;
        }
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private String hash(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
