package com.hamza.stadiumbooking.security.utils;

import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor @Service
public class OwnershipValidationService {
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;

    public String getCurrentUserEmail(){
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
    }

    public UUID getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()){
            throw new IllegalStateException("User not authenticated.");
        }
        Object principal = authentication.getPrincipal();
        if(principal instanceof CustomUserDetails userDetails){
            return userDetails.getId();
        }
        else if (principal instanceof User user) {
            return user.getId();
        }
        throw new ResourceNotFoundException("Could not retrieve user ID from security context.");
    }


    public boolean isAdmin(){
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getAuthorities()
                .stream().anyMatch(a-> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
    }

    public void checkOwnership(UUID stadiumId) {
        if (isAdmin()) {
            return;
        }
        UUID currentUserId = getCurrentUserId();
        boolean isOwner = stadiumRepository.existsByIdAndOwner_Id(stadiumId, currentUserId);
        if (!isOwner) {
            throw new AccessDeniedException("Permission denied. The stadium is not owned by the current manager.");
        }
    }

    public void checkBookingOwnership(UUID bookingUserId){
        UUID currentUserId = getCurrentUserId();
        if (!currentUserId.equals(bookingUserId)) {
            throw new AccessDeniedException("Permission denied. You are not the owner of this booking.");
        }
    }

    public void checkUsership(UUID userId){
        if(isAdmin()){
            return;
        }
        String currentEmail = getCurrentUserEmail();
        boolean isOwner = userRepository.existsByIdAndEmail(userId,currentEmail);
        if(!isOwner){
            throw new AccessDeniedException("Permission denied. for this user profile");
        }
    }

    public User getCurrentUser() {
        return userRepository.findByIdAndIsDeletedFalse(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found in database"));
    }

    public boolean isPlayer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_PLAYER.name()));
    }

    public boolean isStadiumOwner(UUID stadiumId) {
        if (isAdmin()) {
            return true;
        }
        UUID currentUserId = getCurrentUserId();
        return stadiumRepository.existsByIdAndOwner_Id(stadiumId, currentUserId);
    }
}
