package com.hamza.stadiumbooking.stadium;

public record StadiumRequest(
        String name,
        String location,
        Double pricePerHour,
        Integer ballRentalFee,
        Type type,
        String photoUrl
) {
}