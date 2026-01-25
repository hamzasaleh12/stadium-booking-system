package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.stadium.Type;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryTest {
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private StadiumRepository stadiumRepository;
    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private Stadium savedStadium;
    private Booking booking1;
    private final LocalDateTime T0 = LocalDateTime.of(2027, 1, 1, 10, 0);

    @BeforeEach
    void setUp() {
        User user = new User(
                null, 0L, "hamza", "manger@gmail.com", "01000000000",
                "securePass", LocalDate.now(), null, null, Role.ROLE_PLAYER, false
        );
        savedUser = userRepository.save(user);

        Stadium stadium = new Stadium(
                null, 0L, "AL-AHLY", "Nasr_city", 500.00, "image.com",
                Type.ELEVEN_A_SIDE, 50, LocalTime.of(9, 0), LocalTime.of(23, 0),
                null, new HashSet<>(), savedUser, false, null, null
        );
        savedStadium = stadiumRepository.save(stadium);

        booking1 = new Booking(
                null, 0L, T0, T0.plusHours(2), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
    }

    @Test
    void findByUserId() {
        Booking booking2 = new Booking(
                null, 0L, T0.plusHours(2), T0.plusHours(3), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        Page<Booking> bookings = bookingRepository.findByUserId(Pageable.unpaged(), savedUser.getId());

        assertThat(bookings).isNotNull();
        assertThat(bookings).hasSize(2);
        assertThat(bookings.stream().allMatch(b -> b.getUser().getId().equals(savedUser.getId()))).isTrue();
    }

    @Test
    void findByUserId_ShouldReturnEmptyListWhenNoBookingsExist() {
        Page<Booking> bookings = bookingRepository.findByUserId(Pageable.unpaged(), savedUser.getId());
        assertThat(bookings).isEmpty();
    }

    @Test
    void findByStadiumId() {
        Booking booking2 = new Booking(
                null, 0L, T0.plusHours(2), T0.plusHours(3), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        Page<Booking> bookings = bookingRepository.findByStadiumId(Pageable.unpaged(), savedStadium.getId());

        assertThat(bookings).isNotNull();
        assertThat(bookings).hasSize(2);
        assertThat(bookings.stream().allMatch(b -> b.getStadium().getId().equals(savedStadium.getId()))).isTrue();
    }

    @Test
    void findByUserIdAndStadiumId() {
        Booking booking2 = new Booking(
                null, 0L, T0.plusHours(2), T0.plusHours(3), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        Page<Booking> bookings = bookingRepository.findByUserIdAndStadiumId(
                Pageable.unpaged(), savedUser.getId(), savedStadium.getId()
        );

        assertThat(bookings.stream().allMatch(booking ->
                (booking.getStadium().getId().equals(savedStadium.getId())) &&
                        (booking.getUser().getId().equals(savedUser.getId())))).isTrue();
    }

    @Test
    void findAllByUserId() {
        Booking booking2 = new Booking(
                null, 0L, T0.plusHours(2), T0.plusHours(3), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        Booking savedBooking = bookingRepository.save(booking1);
        bookingRepository.save(booking2);

        Page<Booking> bookings = bookingRepository.findAllByUserId(
                Pageable.unpaged(), savedBooking.getUser().getId()
        );

        assertThat(bookings).isNotNull();
        assertThat(bookings).hasSize(2);
        assertThat(bookings.stream().allMatch(b -> b.getUser().getEmail().equals(savedUser.getEmail()))).isTrue();
    }

    @Test
    void testFindConflictingBookingsForNew() {
        Booking booking2 = new Booking(
                null, 0L, T0, T0.plusHours(3), 550.00, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        Booking savedBooking = bookingRepository.save(booking1);

        boolean conflictingBookings = bookingRepository.findConflictingBookingsForNew(
                savedBooking.getStadium().getId(), booking2.getStartTime(), booking2.getEndTime()
        );

        assertThat(conflictingBookings).isTrue();
    }

    @Test
    void testToNotFindConflictingBookingsForNew() {
        LocalDateTime endTime = T0.plusHours(2);
        LocalDateTime endNewTime = endTime.plusHours(1);

        Booking savedBooking = bookingRepository.save(booking1);

        boolean conflictingBookings = bookingRepository.findConflictingBookingsForNew(
                savedBooking.getStadium().getId(), endTime, endNewTime
        );

        assertThat(conflictingBookings).isFalse();
    }

    @Test
    void testFindConflictingBookings_StartOverlap() {
        booking1.setStartTime(T0.plusHours(1));
        booking1.setEndTime(T0.plusHours(3));
        bookingRepository.save(booking1);

        boolean conflictingBookings = bookingRepository.findConflictingBookingsForNew(
                savedStadium.getId(), T0.plusMinutes(30), T0.plusHours(2).plusMinutes(30)
        );

        assertThat(conflictingBookings).isTrue();
    }

    @Test
    void testFindConflictingBookings_EnvelopingConflict() {
        booking1.setStartTime(T0.plusHours(1));
        booking1.setEndTime(T0.plusHours(2));
        bookingRepository.save(booking1);

        boolean conflictingBookings = bookingRepository.findConflictingBookingsForNew(
                savedStadium.getId(), T0.plusMinutes(30), T0.plusHours(2).plusMinutes(30)
        );

        assertThat(conflictingBookings).isTrue();
    }

    @Test
    void testFindConflictingBookingsForUpdate() {
        booking1.setStartTime(T0);
        booking1.setEndTime(T0.plusHours(1));
        Booking existingBooking = bookingRepository.save(booking1);

        Booking otherBooking = new Booking(
                null, 0L, T0.plusHours(1), T0.plusHours(2), 550.0, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        bookingRepository.save(otherBooking);

        boolean hasConflict = bookingRepository.findConflictingBookingsForUpdate(
                existingBooking.getId(), existingBooking.getStadium().getId(), T0, T0.plusHours(2)
        );

        assertThat(hasConflict).isTrue();
    }

    @Test
    void updateExpiredBookings_ShouldMarkPastBookingsAsCompleted() {
        Booking expiredBooking = new Booking(
                null, 0L, T0.minusDays(1), T0.minusDays(1).plusHours(1), 500.0, "Note",
                savedUser, savedStadium, BookingStatus.CONFIRMED, null, null
        );
        Booking savedExpiredBooking = bookingRepository.save(expiredBooking);

        booking1.setStartTime(T0.plusDays(1));
        booking1.setEndTime(T0.plusDays(1).plusHours(1));
        bookingRepository.save(booking1);

        int updatedCount = bookingRepository.updateExpiredBookings(T0);

        assertThat(updatedCount).isEqualTo(1);
        Booking checkBooking = bookingRepository.findById(savedExpiredBooking.getId()).get();
        assertThat(checkBooking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }
}