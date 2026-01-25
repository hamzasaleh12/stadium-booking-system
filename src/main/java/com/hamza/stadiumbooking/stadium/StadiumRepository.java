package com.hamza.stadiumbooking.stadium;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium, UUID> {

    Page<Stadium> findAllByIsDeletedFalse(Pageable pageable);

    Optional<Stadium> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByIdAndOwner_Id(UUID id, UUID owner_id);

    @Query("SELECT DISTINCT s.location FROM Stadium s WHERE s.isDeleted = false")
    List<String> findAllDistinctLocations();
}