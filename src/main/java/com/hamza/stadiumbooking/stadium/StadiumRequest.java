package com.hamza.stadiumbooking.stadium;

import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.Set;

public record StadiumRequest(
        @NotBlank(message = "Stadium name is required")
        String name,

        @NotBlank(message = "Location is required")
        String location,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        Double pricePerHour,

        @NotNull(message = "Ball rental fee is required")
        @Min(value = 0, message = "Rental fee cannot be negative")
        Integer ballRentalFee,

        @NotNull(message = "Opening time is required")
        LocalTime openTime,

        @NotNull(message = "Closing time is required")
        LocalTime closeTime,

        Set<String> features,

        @NotNull(message = "Type is required")
        Type type,

        @NotBlank(message = "Photo URL is required")
        String photoUrl
) {
}