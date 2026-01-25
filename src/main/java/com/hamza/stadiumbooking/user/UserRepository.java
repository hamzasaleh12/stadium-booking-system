package com.hamza.stadiumbooking.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByIdAndEmail(UUID userId , String email);

    boolean existsByPhoneNumberAndIsDeletedFalse(String phoneNumber);

    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    UUID getUserIdByEmail(String email);
}