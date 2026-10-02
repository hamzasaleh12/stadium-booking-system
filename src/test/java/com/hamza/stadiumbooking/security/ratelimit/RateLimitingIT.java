package com.hamza.stadiumbooking.security.ratelimit;

import com.hamza.stadiumbooking.base.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Distributed rate limiting integration flow")
class RateLimitingIT extends AbstractIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String PROTECTED_ENDPOINT = "/api/v1/users/00000000-0000-0000-0000-000000000001";
    private static final String INVALID_LOGIN = """
            {"email":"invalid-email","password":"bad"}
            """;

    @BeforeEach
    void clearRateLimitState() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should allow the first five auth requests")
    void authRequestsBelowLimit_ShouldBeAllowedByRateLimiter() throws Exception {
        for (int requestNumber = 1; requestNumber <= 5; requestNumber++) {
            performLogin("198.51.100.11").andExpect(status().isBadRequest());
        }

        assertThat(redisTemplate.keys("stadium:security:v1:*")).isNotEmpty();
    }

    @Test
    @DisplayName("Should return 429 from request six through request fifteen")
    void authRequestsSixThroughFifteen_ShouldReturnTooManyRequests() throws Exception {
        for (int requestNumber = 1; requestNumber <= 5; requestNumber++) {
            performLogin("198.51.100.12").andExpect(status().isBadRequest());
        }
        var keys = redisTemplate.keys("stadium:security:v1:*");
        assertThat(keys).hasSize(1);
        assertThat(redisTemplate.opsForValue().get(keys.iterator().next())).isEqualTo("5");
        for (int requestNumber = 6; requestNumber <= 15; requestNumber++) {
            performLogin("198.51.100.12")
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().string("Retry-After", "60"));
        }
    }

    @Test
    @DisplayName("Should ban the IP on request sixteen and keep it banned")
    void authRequestSixteen_ShouldBanIp() throws Exception {
        for (int requestNumber = 1; requestNumber <= 5; requestNumber++) {
            performLogin("198.51.100.13").andExpect(status().isBadRequest());
        }
        for (int requestNumber = 6; requestNumber <= 15; requestNumber++) {
            performLogin("198.51.100.13").andExpect(status().isTooManyRequests());
        }

        performLogin("198.51.100.13").andExpect(status().isForbidden());
        performLogin("198.51.100.13").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should apply the business tier before JWT parsing")
    void protectedRequestsWithInvalidJwt_ShouldBeRateLimitedBeforeJwtAuthentication() throws Exception {
        for (int requestNumber = 1; requestNumber <= 30; requestNumber++) {
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.14");
                                return request;
                            })
                            .header("Authorization", "Bearer malformed-token"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(get(PROTECTED_ENDPOINT)
                        .with(request -> {
                            request.setRemoteAddr("198.51.100.14");
                            return request;
                        })
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }

    @Test
    @DisplayName("Should bypass rate limiting for CORS preflight requests")
    void optionsRequests_ShouldBypassRateLimiting() throws Exception {
        for (int requestNumber = 1; requestNumber <= 20; requestNumber++) {
            mockMvc.perform(options(LOGIN_ENDPOINT)
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.15");
                                return request;
                            }))
                    .andExpect(status().isOk());
        }

        assertThat(redisTemplate.keys("stadium:security:v1:*")).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions performLogin(String ip) throws Exception {
        return mockMvc.perform(post(LOGIN_ENDPOINT)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_LOGIN));
    }
}
