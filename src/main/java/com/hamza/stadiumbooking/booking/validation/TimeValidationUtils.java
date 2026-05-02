package com.hamza.stadiumbooking.booking.validation;

import java.time.LocalTime;

public class TimeValidationUtils {
    public static boolean isWithinOperatingHours(LocalTime start, LocalTime end, LocalTime openTime, LocalTime closeTime) {
        if (closeTime.isAfter(openTime)) {
            return !start.isBefore(openTime) && !end.isAfter(closeTime);
        } else {
            if (start.isAfter(end)) return !start.isBefore(openTime) && !end.isAfter(closeTime);
            else return !start.isBefore(openTime) || !end.isAfter(closeTime);
        }
    }
}