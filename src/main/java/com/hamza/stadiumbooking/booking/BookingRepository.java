package com.hamza.stadiumbooking.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID; // MODIFIED: Import UUID

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Page<Booking> findByUserId(Pageable pageable, UUID userId);

    Page<Booking> findByStadiumId(Pageable pageable, UUID stadiumId);

    Page<Booking> findByUserIdAndStadiumId(Pageable pageable, UUID userId, UUID stadiumId);

    Page<Booking> findAllByUserId(Pageable pageable, UUID userId);

    @Query("""
        SELECT case WHEN COUNT(b) > 0 then true ELSE false END
        FROM Booking b\s
        WHERE b.stadium.id = :stadiumId
        AND b.status = 'CONFIRMED'
        AND (:endTime > b.startTime AND :startTime < b.endTime)
   \s""")
    boolean findConflictingBookingsForNew(
            @Param("stadiumId") UUID stadiumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT case WHEN COUNT(b) > 0 then true ELSE false END
        FROM Booking b\s
        WHERE b.stadium.id = :stadiumId
        AND b.status = 'CONFIRMED'
        AND b.id != :bookingId \s
        AND (:endTime > b.startTime AND :startTime < b.endTime)
   \s""")
    boolean findConflictingBookingsForUpdate(
            @Param("bookingId") UUID bookingId,
            @Param("stadiumId") UUID stadiumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Booking b
        SET b.status = 'COMPLETED'
        WHERE b.status = 'CONFIRMED'
        AND b.endTime < :now
    """)
    int updateExpiredBookings(@Param("now") LocalDateTime now);
}