package com.hamza.stadiumbooking.security.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hamza.stadiumbooking.exception.InvalidRefreshTokenException;
import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    public InternalAuthResult refreshToken(String refreshToken) {
        DecodedJWT decodedJWT = jwtProvider.decodedJWT(refreshToken, "REFRESH");
        UUID userId = jwtProvider.getUserId(decodedJWT);
        log.debug("AUTH_REFRESH_TOKEN_VERIFIED userId={}", userId);

        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.warn("AUTH_REFRESH_REJECTED userId={} reason=user_not_found_or_deleted", userId);
                    return new InvalidRefreshTokenException();
                });

        String replacementRefreshToken = jwtProvider.createRefreshToken(user.getEmail(), user.getId());

        if (!refreshTokenStore.rotate(
                user.getId(),
                refreshToken,
                replacementRefreshToken,
                jwtProvider.getRefreshTokenTtl())) {
            log.warn("AUTH_REFRESH_REJECTED userId={} reason=missing_or_replayed_session", userId);
            throw new InvalidRefreshTokenException();
        }

        String newAccessToken = jwtProvider.createAccessToken(
                user.getEmail(),
                user.getId(),
                user.isDeleted(),
                List.of(user.getRole().name())
        );

        log.info("AUTH_REFRESH_ROTATED userId={} ttlSeconds={}",
                userId, jwtProvider.getRefreshTokenTtl().toSeconds());
        return new InternalAuthResult(newAccessToken, replacementRefreshToken);
    }


    public InternalAuthResult login(LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String accessToken = jwtProvider.createAccessToken(
                user.getUsername(),
                user.getId(),
                !user.isEnabled(),
                roles
        );

        String refreshToken = jwtProvider.createRefreshToken(user.getUsername(), user.getId());
        refreshTokenStore.replace(
                user.getId(),
                refreshToken,
                jwtProvider.getRefreshTokenTtl()
        );

        log.info("AUTH_LOGIN_SESSION_CREATED userId={} ttlSeconds={}",
                user.getId(), jwtProvider.getRefreshTokenTtl().toSeconds());
        return new InternalAuthResult(accessToken, refreshToken);
    }

    public void logout(String refreshToken) {
        try {
            DecodedJWT decodedJWT = jwtProvider.decodedJWT(refreshToken, "REFRESH");
            UUID userId = jwtProvider.getUserId(decodedJWT);
            refreshTokenStore.delete(userId);
            log.info("AUTH_LOGOUT_SESSION_DELETED userId={}", userId);
        } catch (JWTVerificationException | InvalidRefreshTokenException ignored) {
            // Logout is intentionally idempotent; the client cookie is cleared by the controller.
            log.debug("AUTH_LOGOUT_NO_VALID_REFRESH_SESSION");
        }
    }
}