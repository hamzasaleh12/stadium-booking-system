package com.hamza.stadiumbooking.scheduler;

import com.hamza.stadiumbooking.booking.BookingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingStatusScheduler {
    private final BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    @Transactional
    public void completeFinishedBookings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<UUID> expiredIds = bookingRepository.findExpiredBookingIds(now);
            if (!expiredIds.isEmpty()) {
                log.info("🔔 Update Job: {} bookings found expired and moved to COMPLETED at {}", expiredIds.size(), now);

                log.debug("Detailed IDs for completed bookings: {}", expiredIds);

                bookingRepository.updateStatusToCompleted(expiredIds);
            }
        } catch (Exception e) {
            log.error("❌ Error during booking status update task: {}", e.getMessage());
        }
    }
}
