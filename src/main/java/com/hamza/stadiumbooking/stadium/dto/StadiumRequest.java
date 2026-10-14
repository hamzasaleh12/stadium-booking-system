package com.hamza.stadiumbooking.stadium.dto;

import com.hamza.stadiumbooking.stadium.Type;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.Set;

public record StadiumRequest(
        @NotBlank(message = "Stadium name is required")
        @Size(max = 100, message = "Stadium name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must not exceed 200 characters")
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

        @Size(max = 20, message = "A stadium cannot have more than 20 features")
        Set<@NotBlank(message = "Features cannot be blank")
                @Size(max = 50, message = "Each feature must not exceed 50 characters") String> features,

        @NotNull(message = "Type is required")
        Type type,

        @NotBlank(message = "Photo URL is required")
        @Size(max = 2048, message = "Photo URL must not exceed 2048 characters")
        String photoUrl
) {
}