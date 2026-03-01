package com.hamza.stadiumbooking.security.auth;

public record InternalAuthResult(
        String accessToken,
        String refreshToken
) {
}
