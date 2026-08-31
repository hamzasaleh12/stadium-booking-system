package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingMapperTest {

    private final BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);

    @Test
    void shouldMapBookingToResponse() {
        UUID userId = UUID.randomUUID();
        UUID stadiumId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .name("Alice")
                .role(Role.ROLE_PLAYER)
                .build();

        Stadium stadium = Stadium.builder()
                .id(stadiumId)
                .name("Main Stadium")
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .startTime(LocalDateTime.of(2026, 9, 10, 18, 0))
                .endTime(LocalDateTime.of(2026, 9, 10, 19, 0))
                .totalPrice(150.0)
                .status(BookingStatus.CONFIRMED)
                .user(user)
                .stadium(stadium)
                .note("Friendly match")
                .build();

        BookingResponse response = bookingMapper.toResponse(booking);

        assertEquals(bookingId, response.id());
        assertEquals(stadiumId, response.stadiumId());
        assertEquals("Main Stadium", response.stadiumName());
        assertEquals(userId, response.userId());
        assertEquals("Alice", response.userName());
        assertEquals("Friendly match", response.note());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Booking booking = Booking.builder()
                .startTime(LocalDateTime.of(2026, 9, 10, 18, 0))
                .endTime(LocalDateTime.of(2026, 9, 10, 19, 0))
                .note("old note")
                .build();

        BookingRequestForUpdate request = new BookingRequestForUpdate(
                null,
                LocalDateTime.of(2026, 9, 11, 19, 0),
                LocalDateTime.of(2026, 9, 11, 20, 30),
                "new note"
        );

        BookingUpdateContext ctx = new BookingUpdateContext(
                LocalDateTime.of(2026, 9, 11, 19, 0),
                LocalDateTime.of(2026, 9, 11, 20, 30),
                null
        );

        bookingMapper.updateBookingFromRequest(request, booking, ctx);

        assertEquals(LocalDateTime.of(2026, 9, 11, 19, 0), booking.getStartTime());
        assertEquals(LocalDateTime.of(2026, 9, 11, 20, 30), booking.getEndTime());
        assertEquals("new note", booking.getNote());
        assertNull(booking.getStadium());
    }

    @Test
    void shouldIgnoreBlankStringsOnUpdate() {
        Booking booking = Booking.builder()
                .startTime(LocalDateTime.of(2026, 9, 10, 18, 0))
                .endTime(LocalDateTime.of(2026, 9, 10, 19, 0))
                .note("old note")
                .build();

        BookingRequestForUpdate request = new BookingRequestForUpdate(
                null,
                null,
                null,
                "   "
        );

        BookingUpdateContext ctx = new BookingUpdateContext(
                LocalDateTime.of(2026, 9, 10, 18, 0),
                LocalDateTime.of(2026, 9, 10, 19, 0),
                null
        );

        bookingMapper.updateBookingFromRequest(request, booking, ctx);

        assertEquals(LocalDateTime.of(2026, 9, 10, 18, 0), booking.getStartTime());
        assertEquals(LocalDateTime.of(2026, 9, 10, 19, 0), booking.getEndTime());
        assertEquals("old note", booking.getNote());
    }
}
