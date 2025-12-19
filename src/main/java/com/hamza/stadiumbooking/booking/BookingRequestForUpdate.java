package com.hamza.stadiumbooking.booking;

import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

public record BookingRequestForUpdate(
        Long stadiumId,

        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @Future(message = "End time must be in the future")
        LocalDateTime endTime
) {
}
