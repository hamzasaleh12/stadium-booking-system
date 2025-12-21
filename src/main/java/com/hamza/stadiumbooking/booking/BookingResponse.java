package com.hamza.stadiumbooking.booking;

import java.time.LocalDateTime;

public record BookingResponse (
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double totalPrice,
        BookingStatus status,
        Long stadiumId,
        String stadiumName,
        Long userId,
        String userName
){
}
