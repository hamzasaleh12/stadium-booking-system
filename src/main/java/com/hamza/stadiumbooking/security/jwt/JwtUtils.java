package com.hamza.stadiumbooking.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value; // استيراد Value
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String username,Long userId, String issuer, List<String> roles) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
                .withIssuer(issuer)
                .withClaim("roles", roles)
                .withClaim("id",userId)
                .sign(getAlgorithm());
    }

    public String createRefreshToken(String username, String issuer) {
        return JWT.create()
                .withSubject(username)
                .withExpiresAt(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                .withIssuer(issuer)
                .sign(getAlgorithm());
    }

    public DecodedJWT decodedJWT(String token) {
        JWTVerifier verifier = JWT.require(getAlgorithm()).build();
        return verifier.verify(token);
    }
}
