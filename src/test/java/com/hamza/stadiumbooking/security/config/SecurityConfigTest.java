package com.hamza.stadiumbooking.security.config;

import com.hamza.stadiumbooking.security.handler.DelegatedAccessDeniedHandler;
import com.hamza.stadiumbooking.security.handler.DelegatedAuthenticationEntryPoint;
import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import com.hamza.stadiumbooking.security.ratelimit.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsConfiguration_ShouldAllowNoOriginsUntilFrontendIsConfigured() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtProvider.class),
                mock(HandlerExceptionResolver.class),
                mock(DelegatedAuthenticationEntryPoint.class),
                mock(DelegatedAccessDeniedHandler.class),
                mock(RateLimiterService.class)
        );

        var corsConfiguration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertThat(corsConfiguration).isNotNull();
        assertThat(corsConfiguration.getAllowedOrigins()).isEmpty();
        assertThat(corsConfiguration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
