package com.hamza.stadiumbooking.stadium;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StadiumRepository extends JpaRepository<Stadium,Long> {
    Page<Stadium> findAllByIsDeletedFalse(Pageable pageable);
    Optional<Stadium> findByIdAndIsDeletedFalse(Long id);

    boolean existsByIdAndOwner_Id(Long id, Long owner_id);

    @Query("SELECT DISTINCT s.location FROM Stadium s")
    List<String> findAllDistinctLocations();
}
