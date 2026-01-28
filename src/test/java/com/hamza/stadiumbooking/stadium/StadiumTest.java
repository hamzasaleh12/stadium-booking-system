package com.hamza.stadiumbooking.stadium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class StadiumTest {

    private Stadium stadium;

    @BeforeEach
    void setUp() {
        stadium = Stadium.builder()
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(22, 0))
                .build();
    }

    // ==========================================
    // SECTION 1: Standard Shift (e.g., 08:00 - 22:00)
    // ==========================================

    @Test
    @DisplayName("Standard: Should return TRUE when booking is within hours")
    void isOpenAt_Standard_Valid() {
        assertThat(stadium.isOpenAt(LocalTime.of(14, 0), LocalTime.of(16, 0))).isTrue();
    }

    @Test
    @DisplayName("Standard: Should return TRUE on exact boundaries")
    void isOpenAt_Standard_Boundaries() {
        assertThat(stadium.isOpenAt(LocalTime.of(8, 0), LocalTime.of(9, 0))).isTrue();

        assertThat(stadium.isOpenAt(LocalTime.of(21, 0), LocalTime.of(22, 0))).isTrue();
    }

    @Test
    @DisplayName("Standard: Should return FALSE when starting too early")
    void isOpenAt_Standard_StartEarly() {
        assertThat(stadium.isOpenAt(LocalTime.of(7, 0), LocalTime.of(9, 0))).isFalse();
    }

    @Test
    @DisplayName("Standard: Should return FALSE when ending too late")
    void isOpenAt_Standard_EndLate() {
        assertThat(stadium.isOpenAt(LocalTime.of(21, 0), LocalTime.of(23, 0))).isFalse();
    }

    @Test
    @DisplayName("Standard: Should return FALSE when completely outside hours")
    void isOpenAt_Standard_Outside() {
        assertThat(stadium.isOpenAt(LocalTime.of(1, 0), LocalTime.of(3, 0))).isFalse();
    }

    // ==========================================
    // SECTION 2: Ramadan Shift (e.g., 20:00 - 02:00)
    // ==========================================

    @Test
    @DisplayName("Ramadan: Should return TRUE when crossing midnight (23:00 -> 01:00)")
    void isOpenAt_Ramadan_CrossingMidnight() {
        stadium.setOpenTime(LocalTime.of(20, 0)); // 8 PM
        stadium.setCloseTime(LocalTime.of(2, 0));  // 2 AM

        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(1, 0);

        assertThat(stadium.isOpenAt(start, end)).isTrue();
    }

    @Test
    @DisplayName("Ramadan: Should return TRUE when inside evening hours (20:00 -> 23:00)")
    void isOpenAt_Ramadan_EveningOnly() {
        stadium.setOpenTime(LocalTime.of(20, 0));
        stadium.setCloseTime(LocalTime.of(2, 0));

        assertThat(stadium.isOpenAt(LocalTime.of(21, 0), LocalTime.of(23, 0))).isTrue();
    }

    @Test
    @DisplayName("Ramadan: Should return TRUE when inside morning hours (00:00 -> 02:00)")
    void isOpenAt_Ramadan_MorningOnly() {
        stadium.setOpenTime(LocalTime.of(20, 0));
        stadium.setCloseTime(LocalTime.of(2, 0));

        assertThat(stadium.isOpenAt(LocalTime.of(0, 0), LocalTime.of(2, 0))).isTrue();
    }

    // ==========================================
    // SECTION 3: Edge Cases & Bug Fixes 🐛
    // ==========================================

    @Test
    @DisplayName("Bug Fix: Should return FALSE if crossing midnight but extending too late (23:00 -> 05:00)")
    void isOpenAt_Ramadan_LateExtension() {
        stadium.setOpenTime(LocalTime.of(20, 0));
        stadium.setCloseTime(LocalTime.of(2, 0));

        LocalTime start = LocalTime.of(23, 0);
        LocalTime end = LocalTime.of(5, 0);

        assertThat(stadium.isOpenAt(start, end)).isFalse();
    }

    @Test
    @DisplayName("Ramadan: Should return FALSE when in the closed gap (Morning)")
    void isOpenAt_Ramadan_ClosedGapMorning() {
        stadium.setOpenTime(LocalTime.of(20, 0));
        stadium.setCloseTime(LocalTime.of(2, 0));

        assertThat(stadium.isOpenAt(LocalTime.of(10, 0), LocalTime.of(12, 0))).isFalse();
    }

    @Test
    @DisplayName("Ramadan: Should return FALSE when in the closed gap (Afternoon)")
    void isOpenAt_Ramadan_ClosedGapAfternoon() {
        stadium.setOpenTime(LocalTime.of(20, 0));
        stadium.setCloseTime(LocalTime.of(2, 0));

        assertThat(stadium.isOpenAt(LocalTime.of(18, 0), LocalTime.of(19, 0))).isFalse();
    }
}