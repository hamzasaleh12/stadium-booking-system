package com.hamza.stadiumbooking.booking;

import com.hamza.stadiumbooking.exception.ConflictingBookingsException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.stadium.Stadium;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final StadiumRepository stadiumRepository;
    private final OwnershipValidationService ownershipValidationService;


    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        Long currentUserId = ownershipValidationService.getCurrentUserId();
        log.info("Action: getMyBookings | Requesting bookings for User ID: {}", currentUserId);

        Page<Booking> bookings = bookingRepository.findAllByUserId(pageable, currentUserId);

        log.info("Action: getMyBookings | Found {} bookings", bookings.getTotalElements());
        return bookings.map(this::mapToDto);
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable, Long stadiumId, Long userId) {
        boolean isAdmin = ownershipValidationService.isAdmin();
        log.info("Action: getAllBookings | Params: stadiumId={}, userId={}, isAdmin={}",
                stadiumId, userId, isAdmin);

        Page<Booking> bookings;
        if (isAdmin) {
            if (stadiumId != null && userId != null) {
                log.debug("Fetching by UserId AND StadiumId (Admin view)");
                bookings = bookingRepository.findByUserIdAndStadiumId(pageable, userId, stadiumId);
            } else if (userId != null) {
                log.debug("Fetching by UserId only (Admin view)");
                bookings = bookingRepository.findByUserId(pageable, userId);
            } else if (stadiumId != null) {
                log.debug("Fetching by StadiumId only (Admin view)");
                bookings = bookingRepository.findByStadiumId(pageable, stadiumId);
            } else {
                log.debug("Fetching ALL bookings (Admin view)");
                bookings = bookingRepository.findAll(pageable);
            }
        } else {
            if (stadiumId == null) {
                log.error("Action: getAllBookings | Error: Missing stadiumId for non-admin user");
                throw new ResourceNotFoundException("you must add stadium id");
            }

            ownershipValidationService.checkOwnership(stadiumId);

            if (userId != null) {
                log.debug("Fetching by UserId AND StadiumId (manger view)");
                bookings = bookingRepository.findByUserIdAndStadiumId(pageable, userId, stadiumId);
            } else {
                log.debug("Fetching by StadiumId only (manger view)");
                bookings = bookingRepository.findByStadiumId(pageable, stadiumId);
            }
        }

        log.info("Action: getAllBookings | Retrieved {} bookings", bookings.getTotalElements());
        return bookings.map(this::mapToDto);
    }

    public BookingResponse getBookingById(Long id) {
        log.info("Action: getBookingById | ID: {}", id);

        Booking booking = bookingRepository.findById(id).orElseThrow(
                () -> {
                    log.error("Action: getBookingById | Booking NOT FOUND with ID: {}", id);
                    return new ResourceNotFoundException("Booking not found with ID: " + id);
                }
        );
        if (!ownershipValidationService.isAdmin()) {
            if (ownershipValidationService.isPlayer()) {
                ownershipValidationService.checkBookingOwnership(booking.getUser().getId());
            } else if (!ownershipValidationService.isStadiumOwner(booking.getStadium().getId())) {
                log.warn("Action: getBookingById | Unauthorized access attempt by Manager/User to see booking: {}", id);
                throw new AccessDeniedException("You can only view bookings for your own stadiums.");
            }
        }
        return mapToDto(booking);
    }


    @Transactional
    public BookingResponse addBooking(BookingRequest bookingRequest) {
        boolean isAfter = bookingRequest.endTime().isAfter(bookingRequest.startTime());
        if (!isAfter) {
            log.warn("Action: addBooking | Invalid Time Range: Start={}, End={}", bookingRequest.startTime(), bookingRequest.endTime());
            throw new IllegalArgumentException("End time must be after start time");
        }

        validateDuration(bookingRequest.startTime(), bookingRequest.endTime());

        User user = ownershipValidationService.getCurrentUser();

        Stadium stadium = stadiumRepository.findByIdAndIsDeletedFalse(bookingRequest.stadiumId()).orElseThrow(
                () -> new ResourceNotFoundException("Stadium not found or is currently closed."));

        boolean hasConflict = bookingRepository.findConflictingBookingsForNew(
                bookingRequest.stadiumId(), bookingRequest.startTime(), bookingRequest.endTime());

        if (hasConflict) {
            log.warn("Action: addBooking | Conflict Detected: StadiumId={}, Start={}, End={}",
                    bookingRequest.stadiumId(), bookingRequest.startTime(), bookingRequest.endTime());
            throw new ConflictingBookingsException("This time is booked");
        }

        Booking booking = mapToEntity(bookingRequest, user, stadium);
        booking.setStatus(BookingStatus.CONFIRMED);

        Booking savedBook = bookingRepository.save(booking);

        log.info("Action: addBooking | Success | Booking Created with ID: {}", savedBook.getId());
        return mapToDto(savedBook);
    }

    @Transactional
    public void deleteBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with ID: " + bookingId)
        );

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Action: deleteBooking | Already Cancelled | ID: {}", bookingId);
            throw new IllegalStateException("Booking is already cancelled.");
        }

        if (!ownershipValidationService.isAdmin()) {
            ownershipValidationService.checkBookingOwnership(booking.getUser().getId());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        // TO READ ONLY
        bookingRepository.save(booking);

        log.info("Action: deleteBooking | Success | Booking Cancelled ID: {}", bookingId);
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId, BookingRequestForUpdate request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(
                () -> new ResourceNotFoundException("Booking not found with ID: " + bookingId)
        );

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            log.warn("Action: updateBooking | Invalid Status: {} | ID: {}", booking.getStatus(), bookingId);
            throw new IllegalStateException("Cannot update a cancelled or completed booking.");
        }

        Stadium targetStadium = (request.stadiumId() != null)
                ? stadiumRepository.findByIdAndIsDeletedFalse(request.stadiumId()).orElseThrow(() -> new ResourceNotFoundException("Stadium not found with ID: " + request.stadiumId()))
                : booking.getStadium();

        if (!ownershipValidationService.isAdmin()) {
            ownershipValidationService.checkBookingOwnership(booking.getUser().getId());
        }

        LocalDateTime newStartTime = (request.startTime() != null) ? request.startTime() : booking.getStartTime();
        LocalDateTime newEndTime = (request.endTime() != null) ? request.endTime() : booking.getEndTime();

        if (!newEndTime.isAfter(newStartTime)) {
            log.warn("Action: updateBooking | Invalid Time Update: Start={}, End={}", newStartTime, newEndTime);
            throw new IllegalArgumentException("End time must be after start time");
        }

        validateDuration(newStartTime,newEndTime);

        boolean hasConflict = bookingRepository.findConflictingBookingsForUpdate(bookingId,
                targetStadium.getId(), newStartTime, newEndTime);

        if (hasConflict) {
            log.warn("Action: updateBooking | Conflict Detected: ID={}, StadiumId={}, Start={}, End={}",
                    bookingId, targetStadium.getId(), newStartTime, newEndTime);
            throw new ConflictingBookingsException("This time is booked");
        }

        booking.setEndTime(newEndTime);
        booking.setStartTime(newStartTime);
        booking.setStadium(targetStadium);

        updateBookingPrice(booking);

        // TO READ ONLY
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Action: updateBooking | Success | Booking Updated ID: {}", savedBooking.getId());
        return mapToDto(savedBooking);
    }

    // ================= HELPER METHODS =================

    private Booking mapToEntity(BookingRequest bookingRequest, User user, Stadium stadium) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStadium(stadium);
        booking.setStartTime(bookingRequest.startTime());
        booking.setEndTime(bookingRequest.endTime());
        updateBookingPrice(booking);
        return booking;
    }

    private BookingResponse mapToDto(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getStadium().getId(),
                booking.getStadium().getName(),
                booking.getUser().getId(),
                booking.getUser().getName()
        );
    }
    private void validateDuration(LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        double hours = minutes / 60.0;

        if (hours < 1.0) throw new IllegalArgumentException("You can't book for less than an hour.");
        if (hours > 3.0) throw new IllegalArgumentException("The stadium lasts 3 hours");

        if (minutes % 30 != 0) {
            throw new IllegalArgumentException("The reservation must be for full hours or for half an hour only.");
        }
    }
    private void updateBookingPrice(Booking booking) {
        Stadium stadium = booking.getStadium();
        double hours = booking.getDuration();
        double price = (hours * stadium.getPricePerHour()) + stadium.getBallRentalFee();
        booking.setTotalPrice(price);
    }
}