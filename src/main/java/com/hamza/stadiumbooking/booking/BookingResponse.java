package com.hamza.stadiumbooking.booking;

import java.time.LocalDateTime;

public record BookingResponse (
        Long id,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Double totalPrice,
        Long stadiumId,
        String stadiumName,
        Long userId,
        String userName
){
}
