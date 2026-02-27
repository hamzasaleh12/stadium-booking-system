package com.hamza.stadiumbooking.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtProviderTest {
    private JwtProvider jwtProvider;
    private final String secret = "secret1234567890secret1234567890";
    private final String issuer = "stadium_booking_system";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(secret, issuer);
    }

    @Test
    @DisplayName("Should create a valid Access Token with all claims")
    void createAccessToken_HappyPath_ShouldSucceed() {
        String username = "hamza@gmail.com";
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("ROLE_USER");

        String token = jwtProvider.createAccessToken(username, userId, false, roles);
        DecodedJWT decoded = getVerifier().verify(token);

        assertThat(token).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo(username);
        assertThat(decoded.getIssuer()).isEqualTo(issuer);
        assertThat(decoded.getClaim("id").asString()).isEqualTo(userId.toString());
        assertThat(decoded.getClaim("roles").asList(String.class)).hasSize(1).contains("ROLE_USER");
    }

    @Test
    @DisplayName("Should create Access Token even if roles list is empty")
    void createAccessToken_EmptyRoles_ShouldSucceed() {
        String token = jwtProvider.createAccessToken("user", UUID.randomUUID(), false, List.of());
        DecodedJWT decoded = getVerifier().verify(token);

        assertThat(decoded.getClaim("roles").asList(String.class)).isEmpty();
    }

    @Test
    @DisplayName("Access Token should expire after exactly 10 minutes")
    void createAccessToken_Expiration_ShouldBeTenMinutes() {
        String token = jwtProvider.createAccessToken("user", UUID.randomUUID(), false, List.of());
        DecodedJWT decoded = getVerifier().verify(token);

        long expectedExp = System.currentTimeMillis() + (10 * 60 * 1000);
        assertThat(decoded.getExpiresAt().getTime()).isCloseTo(expectedExp, within(5000L));
    }

    @Test
    @DisplayName("Refresh Token should be valid and have no roles")
    void createRefreshToken_HappyPath_ShouldSucceed() {
        String username = "hamza@gmail.com";
        String token = jwtProvider.createRefreshToken(username);
        DecodedJWT decoded = getVerifier().verify(token);

        assertThat(decoded.getSubject()).isEqualTo(username);
        assertThat(decoded.getClaim("roles").isMissing()).isTrue();
    }

    @Test
    @DisplayName("Refresh Token should expire after 30 days")
    void createRefreshToken_Expiration_ShouldBeThirtyDays() {
        String token = jwtProvider.createRefreshToken("user");
        DecodedJWT decoded = getVerifier().verify(token);

        long expectedExp = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000);
        assertThat(decoded.getExpiresAt().getTime()).isCloseTo(expectedExp, within(10000L));
    }

    @Test
    @DisplayName("Should throw exception if trying to use REFRESH token as ACCESS token")
    void decodedJWT_WrongTokenType_ShouldThrowException() {
        String refreshToken = jwtProvider.createRefreshToken("hamza");

        assertThatThrownBy(() -> jwtProvider.decodedJWT(refreshToken, "ACCESS"))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    @DisplayName("Should throw exception if token is tampered (Modified)")
    void decodedJWT_TamperedToken_ShouldThrowException() {
        String validToken = jwtProvider.createAccessToken("user", UUID.randomUUID(), false,List.of());
        String tamperedToken = validToken + "xyz";

        assertThatThrownBy(() -> jwtProvider.decodedJWT(tamperedToken,"ACCESS"))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    @DisplayName("Should handle Null roles by creating a token with null claim")
    void createAccessToken_NullRoles_ShouldStillWork() {
        String token = jwtProvider.createAccessToken("user", UUID.randomUUID(), false,null);
        DecodedJWT decoded = getVerifier().verify(token);

        assertThat(decoded.getClaim("roles").isNull()).isTrue();
    }

    @Test
    void getAuthenticate_ShouldReturnCorrectData() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.createAccessToken("hamza", userId, false, List.of("ROLE_USER"));

        UsernamePasswordAuthenticationToken authenticate = jwtProvider.getAuthenticate(token);
        CustomUserDetails principal = (CustomUserDetails) authenticate.getPrincipal();

        assertThat(authenticate.getName()).isEqualTo("hamza");
        assertThat(authenticate.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(authenticate.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()).isEqualTo(List.of("ROLE_USER"));
        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void getAuthenticate_ShouldReturnCorrectData_whenRolesIsNull() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.createAccessToken("hamza", userId, false, null);

        UsernamePasswordAuthenticationToken authenticate = jwtProvider.getAuthenticate(token);
        CustomUserDetails principal = (CustomUserDetails) authenticate.getPrincipal();

        assertThat(authenticate.getName()).isEqualTo("hamza");
        assertThat(authenticate.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(authenticate.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()).isEqualTo(List.of());
        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.isEnabled()).isTrue();
    }

    @Test
    void getAuthenticate_ShouldReturnCorrectData_whenIdIsNull() {
        String tokenWithoutId = com.auth0.jwt.JWT.create()
                .withSubject("hamza@gmail.com")
                .withClaim("type", "ACCESS")
                .withIssuer(issuer)
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(secret));

        assertThatThrownBy(() -> jwtProvider.getAuthenticate(tokenWithoutId))
                .isInstanceOf(com.auth0.jwt.exceptions.JWTVerificationException.class)
                .hasMessageContaining("Invalid Token");
    }

    // Helper method
    private JWTVerifier getVerifier() {
        return JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).build();
    }
}