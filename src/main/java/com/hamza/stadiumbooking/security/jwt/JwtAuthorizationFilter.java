package com.hamza.stadiumbooking.security.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtils utils;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if(request.getServletPath().equals("/api/v1/login") || request.getServletPath().equals("/api/v1/auth/refresh-token")){
            filterChain.doFilter(request,response);
        } else {
            String authorizationHeader = request.getHeader(AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                try {
                    String token = authorizationHeader.substring("Bearer ".length());
                    DecodedJWT decodedJWT = utils.decodedJWT(token);
                    String email = decodedJWT.getSubject();
                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);

                    String userIdString = decodedJWT.getClaim("id").asString();
                    UUID userId = UUID.fromString(userIdString);

                    Collection<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    CustomUserDetails userDetails = new CustomUserDetails(
                            userId,
                            email,
                            "",
                            authorities
                    );
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    filterChain.doFilter(request, response);

                } catch (IllegalArgumentException | JWTVerificationException e) {
                    exceptionResolver.resolveException(request,response,null,e);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }
}
