package com.hamza.stadiumbooking.security.auth;

import com.hamza.stadiumbooking.security.jwt.JwtProvider;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
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
            throw new IllegalArgumentException("Missing refresh token cookie.");
        }
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }
}