package com.hamza.stadiumbooking.stadium;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StadiumMapperTest {

    private final StadiumMapper stadiumMapper = Mappers.getMapper(StadiumMapper.class);

    @Test
    void shouldMapStadiumToResponse() {
        Stadium stadium = Stadium.builder()
                .id(UUID.randomUUID())
                .name("Stadium One")
                .location("Cairo")
                .pricePerHour(120.0)
                .ballRentalFee(20)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(22, 0))
                .features(Set.of("lights", "parking"))
                .type(Type.ELEVEN_A_SIDE)
                .photoUrl("https://example.com/pic.jpg")
                .build();

        StadiumResponse response = stadiumMapper.toResponse(stadium);

        assertEquals(stadium.getId(), response.id());
        assertEquals("Stadium One", response.name());
        assertEquals("Cairo", response.location());
        assertEquals(120.0, response.pricePerHour());
        assertEquals(Set.of("lights", "parking"), response.features());
        assertEquals(Type.ELEVEN_A_SIDE, response.type());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Stadium stadium = Stadium.builder()
                .name("Old Name")
                .location("Alex")
                .pricePerHour(100.0)
                .ballRentalFee(5)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(20, 0))
                .features(new java.util.HashSet<>(Set.of("old")))
                .photoUrl("old.jpg")
                .type(Type.ELEVEN_A_SIDE)
                .build();

        StadiumRequestForUpdate request = new StadiumRequestForUpdate(
                "New Name",
                180.0,
                15,
                LocalTime.of(10, 0),
                LocalTime.of(23, 0),
                Set.of("new", "parking"),
                Type.FIVE_A_SIDE,
                "new.jpg"
        );

        StadiumUpdateContext ctx = new StadiumUpdateContext(Set.of("new", "parking"));
        stadiumMapper.updateStadiumFromRequest(request, stadium, ctx);

        assertEquals("New Name", stadium.getName());
        assertEquals(180.0, stadium.getPricePerHour());
        assertEquals(15, stadium.getBallRentalFee());
        assertEquals(LocalTime.of(10, 0), stadium.getOpenTime());
        assertEquals(LocalTime.of(23, 0), stadium.getCloseTime());
        assertEquals(Set.of("new", "parking"), stadium.getFeatures());
        assertEquals(Type.FIVE_A_SIDE, stadium.getType());
        assertEquals("new.jpg", stadium.getPhotoUrl());
        assertEquals("Alex", stadium.getLocation());
    }
}
