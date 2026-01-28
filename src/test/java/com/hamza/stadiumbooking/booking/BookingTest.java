package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.stadium.Stadium;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BookingTest {

    private Booking booking;

    @BeforeEach
    void setUp() {
        Stadium stadium = Stadium.builder()
                .pricePerHour(100.0)
                .ballRentalFee(20)
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(23, 0))
                .build();

        booking = Booking.builder()
                .stadium(stadium)
                .startTime(LocalDateTime.of(2027, 1, 1, 10, 0))
                .endTime(LocalDateTime.of(2027, 1, 1, 12, 0))
                .status(BookingStatus.CONFIRMED)
                .build();
    }

    // --- Duration Calculation Tests ---
    @Test
    void getDuration_ShouldReturnCorrectDuration() {
        assertThat(booking.getDuration()).isEqualTo(2.0);
    }

    @Test
    void getDuration_ShouldReturnZero_WhenStartTimeIsNull() {
        booking.setStartTime(null);
        assertThat(booking.getDuration()).isEqualTo(0.0);
    }

    @Test
    void getDuration_ShouldReturnZero_WhenEndTimeIsNull() {
        booking.setEndTime(null);
        assertThat(booking.getDuration()).isEqualTo(0.0);
    }

    // --- Price Calculation Tests ---
    @Test
    void calculateTotalPrice_ForWholeHours() {
        booking.calculateTotalPrice();
        assertThat(booking.getTotalPrice()).isEqualTo(220.0);
    }

    @Test
    void calculateTotalPrice_ForFractionalHours() {
        booking.setEndTime(booking.getStartTime().plusMinutes(90)); // 1.5 hours
        booking.calculateTotalPrice();
        assertThat(booking.getTotalPrice()).isEqualTo(170.0);
    }

    // --- Duration Validation Tests ---

    @Test
    void validateDuration_ShouldPass_WhenValid() {
        assertDoesNotThrow(() -> booking.validateDuration());
    }

    @Test
    void validateDuration_ShouldPass_WhenEndTimeIsNULL() {
        booking.setEndTime(null);
        assertDoesNotThrow(() -> booking.validateDuration());
    }

    @Test
    void validateDuration_ShouldPass_WhenStartTimeIsNull() {
        booking.setStartTime(null);
        assertDoesNotThrow(() -> booking.validateDuration());
    }

    @Test
    void validateDuration_ShouldPass_WhenExactlyOneHour() {
        booking.setEndTime(booking.getStartTime().plusHours(1));
        assertDoesNotThrow(() -> booking.validateDuration());
    }

    @Test
    void validateDuration_ShouldPass_WhenExactlyThreeHours() {
        booking.setEndTime(booking.getStartTime().plusHours(3));
        assertDoesNotThrow(() -> booking.validateDuration());
    }

    @Test
    void validateDuration_ShouldThrow_WhenEndIsBeforeStart() {
        booking.setEndTime(booking.getStartTime().minusHours(1));
        assertThatThrownBy(booking::validateDuration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time.");
    }

    @Test
    void validateDuration_ShouldThrow_WhenLessThanOneHour() {
        booking.setEndTime(booking.getStartTime().plusMinutes(45));
        assertThatThrownBy(booking::validateDuration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You can't book for less than an hour.");
    }

    @Test
    void validateDuration_ShouldThrow_WhenMoreThanThreeHours() {
        booking.setEndTime(booking.getStartTime().plusHours(4));
        assertThatThrownBy(booking::validateDuration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Booking duration cannot exceed 3 hours");
    }

    @Test
    void validateDuration_ShouldThrow_WhenStepIsNotThirtyMinutes() {
        booking.setEndTime(booking.getStartTime().plusMinutes(75)); // 1h 15m
        assertThatThrownBy(booking::validateDuration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("full hours or for half an hour only.");
    }

    // --- Modification Window Tests ---

    @Test
    void isModificationWindowClosed_ShouldReturnTrue_WhenTimeIsNow() {
        booking.setStartTime(LocalDateTime.now());
        assertThat(booking.isModificationWindowClosed()).isTrue();
    }

    @Test
    @DisplayName("Should return TRUE (closed) safely if startTime is NULL (Defensive Check)")
    void isModificationWindowClosed_ShouldReturnTrue_WhenStartTimeIsNull() {
        booking.setStartTime(null);
        boolean isClosed = booking.isModificationWindowClosed();
        assertThat(isClosed).isTrue();
    }

    @Test
    void isModificationWindowClosed_ShouldReturnFalse_WhenMoreThanSixHoursLeft() {
        booking.setStartTime(LocalDateTime.now().plusHours(7));
        assertThat(booking.isModificationWindowClosed()).isFalse();
    }

    @Test
    void isModificationWindowClosed_ShouldReturnTrue_WhenOneMinuteBeforeSixHours() {
        booking.setStartTime(LocalDateTime.now().plusMinutes(6 * 60 - 1));
        assertThat(booking.isModificationWindowClosed()).isTrue();
    }
}