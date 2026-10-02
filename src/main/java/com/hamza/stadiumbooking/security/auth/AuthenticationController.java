package com.hamza.stadiumbooking.security.auth;

import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import com.hamza.stadiumbooking.exception.InvalidRefreshTokenException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        InternalAuthResult authResponse = authenticationService.login(loginRequest);

        ResponseCookie cookie = jwtProvider.createRefreshTokenCookie(authResponse.refreshToken());

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthenticationResponse(authResponse.accessToken()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("AUTH_REFRESH_REJECTED reason=missing_cookie");
            throw new InvalidRefreshTokenException();
        }
        InternalAuthResult response = authenticationService.refreshToken(refreshToken);
        log.debug("AUTH_REFRESH_RESPONSE_ISSUED");
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE,
                        jwtProvider.createRefreshTokenCookie(response.refreshToken()).toString())
                .body(new AuthenticationResponse(response.accessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authenticationService.logout(refreshToken);
        } else {
            log.debug("AUTH_LOGOUT_REQUEST_WITHOUT_COOKIE");
        }
        log.debug("AUTH_LOGOUT_COOKIE_CLEARED");
        return ResponseEntity.noContent()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE,
                        jwtProvider.clearRefreshTokenCookie().toString())
                .build();
    }
}