package com.hamza.stadiumbooking.security.ratelimit;

import com.hamza.stadiumbooking.exception.IpBannedException;
import com.hamza.stadiumbooking.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration BAN_DURATION = Duration.ofHours(2);

    private final RateLimiterService rateLimiterService;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("RATE_LIMIT_BYPASS method=OPTIONS path={}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        try {
            if (rateLimiterService.isBanned(clientIp)) {
                throw new IpBannedException();
            }

            RateLimitTier tier = classify(resolvePath(request));
            RateLimitResult result = rateLimiterService.checkLimit(
                    "rate:ip:" + clientIp + ":" + tier.name().toLowerCase(),
                    tier.maxAllowed(),
                    tier.banThreshold(),
                    WINDOW
            );

            enforceResult(result, clientIp, tier);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException | IpBannedException exception) {
            log.warn("RATE_LIMIT_BLOCKED ip={} path={} tier={} responseStatus={}",
                    clientIp, request.getRequestURI(), classify(resolvePath(request)),
                    exception instanceof IpBannedException ? 403 : 429);
            exceptionResolver.resolveException(request, response, null, exception);
        }
    }

    private void enforceResult(RateLimitResult result, String clientIp, RateLimitTier tier) {
        if (result.banThresholdExceeded()) {
            log.warn("RATE_LIMIT_THRESHOLD_EXCEEDED ip={} tier={} count={} action=ban",
                    clientIp, tier.name(), result.count());
            rateLimiterService.banIp(clientIp, BAN_DURATION);
            throw new IpBannedException();
        }
        if (result.count() > tier.maxAllowed()) {
            log.warn("RATE_LIMIT_SOFT_LIMIT_EXCEEDED ip={} tier={} count={} maxAllowed={} retryAfterSeconds={}",
                    clientIp, tier.name(), result.count(), tier.maxAllowed(), result.retryAfterSeconds());
            throw new RateLimitExceededException(result.retryAfterSeconds());
        }
    }

    private RateLimitTier classify(String path) {
        if (path.startsWith("/api/v1/auth/") || path.equals("/api/v1/users")) {
            return RateLimitTier.AUTH;
        }
        if (path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/actuator/health")
                || path.equals("/")) {
            return RateLimitTier.INFRASTRUCTURE;
        }
        return RateLimitTier.BUSINESS;
    }

    private String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        String requestUri = request.getRequestURI();
        return requestUri == null || requestUri.isBlank() ? "/" : requestUri;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            throw new IllegalStateException("Cannot resolve client IP address");
        }
        return remoteAddress;
    }
}
