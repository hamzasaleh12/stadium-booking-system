package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.exception.ConflictingBookingsException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final StadiumRepository stadiumRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, StadiumRepository stadiumRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.stadiumRepository = stadiumRepository;
    }

    public List<BookingResponse> getAllBookings(Long stadiumId, Long userId) {
        List<Booking> bookings;
        if (stadiumId != null && userId != null) {
            bookings = bookingRepository.findByUserIdAndStadiumId(userId, stadiumId);
        } else if (userId != null) {
            bookings = bookingRepository.findByUserId(userId);
        } else if (stadiumId != null) {
            bookings = bookingRepository.findByStadiumId(stadiumId);
        } else {
            bookings = bookingRepository.findAll();
        }
        return bookings.stream().map(this::mapToDto).toList();
    }

    private BookingResponse mapToDto(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getStartTime(),
                booking.getNumberOfHours(),
                booking.getTotalPrice(),
                booking.getUser().getId(),
                booking.getStadium().getId()
        );
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with ID: " + id)
        );
        return mapToDto(booking);
    }

    public BookingResponse addBooking(BookingRequest bookingRequest) {
        User user = userRepository.findById(bookingRequest.userId()).orElseThrow(
                () -> new ResourceNotFoundException("User not found with ID: " + bookingRequest.userId())
        );
        Stadium stadium = stadiumRepository.findById(bookingRequest.stadiumId()).orElseThrow(
                () -> new ResourceNotFoundException("Stadium not found with ID: " + bookingRequest.stadiumId())
        );


        boolean isAvailable = bookingRepository.findConflictingBookings(
                        bookingRequest.stadiumId(),
                        bookingRequest.startTime(),
                        bookingRequest.startTime().plusHours(bookingRequest.numberOfHours()))
                .isEmpty();

        if (!isAvailable) {
            throw new ConflictingBookingsException("This time is booked");
        }

        Booking booking = mapToEntity(bookingRequest, user, stadium);
        Booking savedBook = bookingRepository.save(booking);
        return mapToDto(savedBook);
    }

    private Booking mapToEntity(BookingRequest bookingRequest, User user, Stadium stadium) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStadium(stadium);
        booking.setStartTime(bookingRequest.startTime());
        booking.setNumberOfHours(bookingRequest.numberOfHours());
        booking.setEndTime(bookingRequest.startTime().plusHours(bookingRequest.numberOfHours()));

        Double price = (bookingRequest.numberOfHours() * stadium.getPricePerHour()) + stadium.getBallRentalFee();
        booking.setTotalPrice(price);
        return booking;
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId,
                                         LocalDateTime startTime,
                                         Integer numberOfHours,
                                         Long stadiumId,
                                         Long userId) {

        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with ID: " + bookingId)
        );

        User targetUser = (userId != null)
                ? userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId))
                : booking.getUser();

        Stadium targetStadium = (stadiumId != null)
                ? stadiumRepository.findById(stadiumId).orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + stadiumId))
                : booking.getStadium();

        LocalDateTime newStartTime = (startTime != null) ? startTime : booking.getStartTime();
        Integer newDuration = (numberOfHours != null) ? numberOfHours : booking.getNumberOfHours();
        LocalDateTime newEndTime = newStartTime.plusHours(newDuration);

        boolean isAvailable = bookingRepository.findConflictingBookings(
                bookingId,
                targetStadium.getId(),
                newStartTime,
                newEndTime
        ).isEmpty();

        if (!isAvailable) {
            throw new ConflictingBookingsException("This time is booked");
        }

        booking.setNumberOfHours(newDuration);
        booking.setStartTime(newStartTime);
        booking.setEndTime(newEndTime);
        booking.setStadium(targetStadium);
        booking.setUser(targetUser);

        Double newPrice = (newDuration * targetStadium.getPricePerHour()) + targetStadium.getBallRentalFee();
        booking.setTotalPrice(newPrice);

        //TO READ ONLY
        Booking savedBooking = bookingRepository.save(booking);
        return mapToDto(savedBooking);
    }

    public void deleteBooking(Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new ResourceNotFoundException("Booking not found with ID: " + bookingId);
        }
        bookingRepository.deleteById(bookingId);
    }
}