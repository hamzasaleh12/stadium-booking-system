package com.hamza.stadiumbooking.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component @Slf4j
public class JwtUtils {
    private final String secretKey;
    private final String issuer;

    public JwtUtils(@Value("${jwt.secret}") String secretKey,
                    @Value("${jwt.issuer}") String issuer) {
        this.secretKey = secretKey;
        this.issuer = issuer;
    }

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String username, UUID userId, boolean isDeleted,List<String> roles) {
        return JWT.create()
                .withSubject(username)
                .withClaim("type", "ACCESS")
                .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .withIssuer(issuer)
                .withClaim("roles", roles)
                .withClaim("id", userId.toString())
                .withClaim("isDeleted", isDeleted)
                .sign(getAlgorithm());
    }

    public String createRefreshToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withClaim("type", "REFRESH")
                .withExpiresAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                .withIssuer(issuer)
                .sign(getAlgorithm());
    }

    public DecodedJWT decodedJWT(String token, String expectedType) {
        JWTVerifier verifier = JWT.require(getAlgorithm())
                .withIssuer(issuer)
                .withClaim("type", expectedType)
                .build();
        return verifier.verify(token);
    }

    public UsernamePasswordAuthenticationToken getAuthenticate(String token) {
        DecodedJWT decodedJWT = decodedJWT(token, "ACCESS");

        UUID userId = Optional.ofNullable(decodedJWT.getClaim("id").asString())
                .map(UUID::fromString)
                .orElseThrow(() -> {
                    log.error("Security Alert: JWT Token for subject [{}] is missing the mandatory 'id' claim.",
                            decodedJWT.getSubject());

                    return new JWTVerificationException("Invalid Token");
                });

        String email = decodedJWT.getSubject();
        boolean isDeleted = decodedJWT.getClaim("isDeleted").asBoolean();
        String[] roles = decodedJWT.getClaim("roles").asArray(String.class);

        Collection<SimpleGrantedAuthority> authorities = (roles == null) ? List.of() :
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();

        CustomUserDetails userDetails = new CustomUserDetails(userId, email, "", isDeleted, authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    }
}