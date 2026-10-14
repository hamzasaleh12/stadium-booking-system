package com.hamza.stadiumbooking.stadium.dto;

import com.hamza.stadiumbooking.stadium.Type;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record StadiumResponse(
        UUID id,
        String name,
        String location,
        Double pricePerHour,
        Integer ballRentalFee,
        LocalTime openTime,
        LocalTime closeTime,
        Set<String> features,
        Type type,
        String photoUrl,
        UUID ownerId
) {
}
