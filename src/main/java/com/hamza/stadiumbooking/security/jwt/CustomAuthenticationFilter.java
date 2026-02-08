package com.hamza.stadiumbooking.security.jwt;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamza.stadiumbooking.security.auth.AuthenticationResponse;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import com.hamza.stadiumbooking.security.auth.LoginRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;


import java.io.IOException;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@RequiredArgsConstructor @Slf4j
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

    private final AuthenticationManager authenticationManager;
    private final JwtUtils utils;
    private final HandlerExceptionResolver exceptionResolver;
    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginRequest loginRequest = mapper.readValue(request.getInputStream(),LoginRequest.class);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password());
            return authenticationManager.authenticate(authenticationToken);
        } catch (IOException e) {
            throw new AuthenticationServiceException("Failed to parse login request", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        if (!(authResult.getPrincipal() instanceof CustomUserDetails user)) {
            throw new ServletException("Principal is not an instance of CustomUserDetails");
        }

        String accessToken = utils.createAccessToken(
                user.getUsername(),
                user.getId(),
                !user.isEnabled(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        String refreshToken = utils.createRefreshToken(user.getUsername());

        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(30L * 24 * 60 * 60)
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
        response.setContentType("application/json");

        mapper.writeValue(response.getOutputStream(), new AuthenticationResponse(accessToken));
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        if (failed instanceof BadCredentialsException) {
            log.warn("Login failed: Wrong password for user");
        } else if (failed instanceof UsernameNotFoundException) {
            log.warn("Login failed: User not found");
        }
        exceptionResolver.resolveException(request,response,null,failed);
    }
}