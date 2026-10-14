package com.hamza.stadiumbooking.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingRequest(
        @NotNull UUID stadiumId,
        @NotNull @Future LocalDateTime startTime,
        @NotNull @Future LocalDateTime endTime,
        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {}
