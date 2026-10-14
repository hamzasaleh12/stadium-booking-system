package com.hamza.stadiumbooking.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingRequestForUpdate(
        UUID stadiumId,

        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @Future(message = "End time must be in the future")
        LocalDateTime endTime,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {
}
