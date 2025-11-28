package com.hamza.stadiumbooking.stadium;

public record StadiumResponse(
        Long id,
        String name,
        String location,
        Double pricePerHour,
        Integer ballRentalFee,
        Type type,
        String photoUrl
) {
}
