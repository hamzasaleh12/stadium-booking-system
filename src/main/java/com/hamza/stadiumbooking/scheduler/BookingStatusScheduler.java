package com.hamza.stadiumbooking.scheduler;

import com.hamza.stadiumbooking.booking.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingStatusScheduler {
    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    @Transactional
    public void completeFinishedBookings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int updatedCount = bookingRepository.updateExpiredBookings(now);
            if (updatedCount > 0) {
                log.info("Job executed: Moved {} bookings to COMPLETED status at {}", updatedCount, now);
            }
        } catch (Exception e) {
            log.error("❌ Error during booking status update task: {}", e.getMessage());
        }
    }
}
