package com.hamza.stadiumbooking.booking;

import java.time.LocalDateTime;

public record BookingRequest(
        Long userId,
        Long stadiumId,
        LocalDateTime startTime,
        Integer numberOfHours
) {
}