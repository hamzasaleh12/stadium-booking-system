package com.hamza.stadiumbooking.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID; // MODIFIED: Import UUID

public record BookingRequest(
        @NotNull UUID stadiumId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        String note
) {}