package com.hamza.stadiumbooking.user;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@(gmail|yahoo|outlook|hotmail)\\.com$",
                message = "Email must be from a valid provider (gmail, yahoo, outlook, hotmail)"
        )
        String email,
        @Pattern(regexp = "^01[0125][0-9]{8}$", message = "Invalid Phone Number")
        String phoneNumber,
        @NotBlank
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,50}$",
                message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character."
        )
        String password,
        @NotNull
        LocalDate dob
) {
}