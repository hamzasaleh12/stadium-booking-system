package com.hamza.stadiumbooking.booking.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.hamza.stadiumbooking.booking.BookingStatus;

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
