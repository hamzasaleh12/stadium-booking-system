package com.hamza.stadiumbooking.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@(gmail|yahoo|outlook|hotmail)\\.com$",
                message = "Email must be from a valid provider"
        )
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
