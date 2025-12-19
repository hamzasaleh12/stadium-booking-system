package com.hamza.stadiumbooking.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Page<User> findAllByIsDeletedFalse(Pageable pageable);
    Optional<User> findByIdAndIsDeletedFalse(Long id);
    boolean existsByIdAndEmail(Long userId , String email);
    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Long getUserIdByEmail(String email);
}
