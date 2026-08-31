package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.stadium.Stadium;

import java.time.LocalDateTime;

public record BookingUpdateContext(
        LocalDateTime newStartTime,
        LocalDateTime newEndTime,
        Stadium targetStadium
) {
}
