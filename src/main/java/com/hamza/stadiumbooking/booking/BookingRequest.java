package com.hamza.stadiumbooking.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull(message = "Stadium ID is required")
        Long stadiumId,
        @NotNull(message = "Start time is required")
        @Future(message = "Booking start time must be in the future")
        LocalDateTime startTime,
        @NotNull(message = "End time is required")
        @Future(message = "Booking end time must be in the future")
        LocalDateTime endTime
) {
}