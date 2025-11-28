package com.hamza.stadiumbooking.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByStadiumId(Long stadiumId);

    List<Booking> findByUserIdAndStadiumId(Long userId, Long stadiumId);

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.stadium.id = :stadiumId
        AND (:endTime > b.startTime AND :startTime < b.endTime)
    """)
    List<Booking> findConflictingBookings(
            @Param("stadiumId") Long stadiumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.stadium.id = :stadiumId
        AND b.id != :bookingId  
        AND (:endTime > b.startTime AND :startTime < b.endTime)
    """)
    List<Booking> findConflictingBookings(
            @Param("bookingId") Long bookingId,
            @Param("stadiumId") Long stadiumId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}