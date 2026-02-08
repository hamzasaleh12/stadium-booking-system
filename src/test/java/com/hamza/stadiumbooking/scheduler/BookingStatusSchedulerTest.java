package com.hamza.stadiumbooking.scheduler;

import com.hamza.stadiumbooking.booking.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingStatusSchedulerTest {
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingStatusScheduler scheduler;

    @Test
    void completeFinishedBookings_whenExpiredFound_shouldUpdateStatus() {
        UUID id = UUID.randomUUID();
        given(bookingRepository.findExpiredBookingIds(any(LocalDateTime.class)))
                .willReturn(List.of(id));

        scheduler.completeFinishedBookings();

        verify(bookingRepository, times(1)).updateStatusToCompleted(List.of(id));
    }

    @Test
    void completeFinishedBookings_whenNoExpiredFound_shouldNotUpdate() {
        given(bookingRepository.findExpiredBookingIds(any(LocalDateTime.class)))
                .willReturn(List.of());

        scheduler.completeFinishedBookings();

        verify(bookingRepository, never()).updateStatusToCompleted(any());
    }

    @Test
    void completeFinishedBookings_shouldHandleException_whenRepositoryThrowsError() {
        given(bookingRepository.findExpiredBookingIds(any(LocalDateTime.class)))
                .willThrow(new RuntimeException("Database Connection Failed"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> scheduler.completeFinishedBookings());

        verify(bookingRepository, never()).updateStatusToCompleted(any());
    }
}