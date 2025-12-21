package com.hamza.stadiumbooking.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UserUpdateRequest(
        String name,

        @Email(message = "Invalid email format")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@(gmail|yahoo|outlook|hotmail)\\.com$",
                message = "Email must be from a valid provider"
        )
        String email,

        @Pattern(regexp = "^01[0125][0-9]{8}$", message = "Invalid Phone Number")
        String phoneNumber,

        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,50}$",
                message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character."
        )
        String password,
        LocalDate dob
) {
}
