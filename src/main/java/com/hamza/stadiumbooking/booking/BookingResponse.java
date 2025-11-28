package com.hamza.stadiumbooking.booking;

import java.time.LocalDateTime;

public record BookingResponse (
        Long id,
        LocalDateTime startTime,
        Integer numberOfHours,
        Double totalPrice,
        Long stadiumId,
        Long userId
){
}
