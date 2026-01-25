package com.hamza.stadiumbooking.booking;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double totalPrice,
        BookingStatus status,
        UUID stadiumId,
        String stadiumName,
        UUID userId,
        String userName,
        String note
) {}
