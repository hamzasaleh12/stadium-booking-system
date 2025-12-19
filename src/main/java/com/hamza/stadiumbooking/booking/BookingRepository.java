package com.hamza.stadiumbooking.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Pageable pageable, Long userId);

    Page<Booking> findByStadiumId(Pageable pageable,Long stadiumId);

    Page<Booking> findByUserIdAndStadiumId(Pageable pageable,Long userId, Long stadiumId);

    Page<Booking> findAllByUserId(Pageable pageable,Long userId);

    @Query("""
        SELECT case WHEN COUNT(b) > 0 then true ELSE false END
        FROM Booking b\s
        WHERE b.stadium.id = :stadiumId
        AND b.status != 'CANCELLED'
        AND (:endTime > b.startTime AND :startTime < b.endTime)
   \s""")
    boolean findConflictingBookingsForNew(
            @Param("stadiumId") Long stadiumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT case WHEN COUNT(b) > 0 then true ELSE false END
        FROM Booking b\s
        WHERE b.stadium.id = :stadiumId
        AND b.status != 'CANCELLED'
        AND b.id != :bookingId \s
        AND (:endTime > b.startTime AND :startTime < b.endTime)
   \s""")
    boolean findConflictingBookingsForUpdate(
            @Param("bookingId") Long bookingId,
            @Param("stadiumId") Long stadiumId,
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