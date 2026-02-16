package com.hamza.stadiumbooking.security.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Missing refresh token cookie.");
        }
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken));
    }
}