package com.hamza.stadiumbooking.stadium.dto;

import com.hamza.stadiumbooking.stadium.Type;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.Set;

public record StadiumRequestForUpdate(
        @Size(max = 100, message = "Stadium name must not exceed 100 characters")
        String name,

        @Positive(message = "Price must be positive")
        Double pricePerHour,

        @Min(value = 0, message = "Rental fee cannot be negative")
        Integer ballRentalFee,

        LocalTime openTime,
        LocalTime closeTime,

        @Size(max = 20, message = "A stadium cannot have more than 20 features")
        Set<@NotBlank(message = "Features cannot be blank")
                @Size(max = 50, message = "Each feature must not exceed 50 characters") String> features,

        Type type,

        @Size(max = 2048, message = "Photo URL must not exceed 2048 characters")
        String photoUrl
) {
}
