package com.hamza.stadiumbooking.stadium;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public record StadiumRequestForUpdate(
        String name,
        @Positive(message = "Price must be positive")
        Double pricePerHour,
        @Min(value = 0, message = "Rental fee cannot be negative")
        Integer ballRentalFee,
        String photoUrl
) {
}