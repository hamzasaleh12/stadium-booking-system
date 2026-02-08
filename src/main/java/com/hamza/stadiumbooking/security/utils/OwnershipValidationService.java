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

import java.util.UUID;

@RequiredArgsConstructor @Service
public class OwnershipValidationService {
    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;

    public String getCurrentUserEmail(){
        return getAuthenticatedUser().getName();
    }

    public UUID getCurrentUserId(){
        Object principal = getAuthenticatedUser().getPrincipal();
        if(principal instanceof CustomUserDetails userDetails){
            return userDetails.getId();
        }
        throw new IllegalStateException("Could not retrieve user ID from security context.");
    }

    public User getCurrentUser() {
        return userRepository.findByIdAndIsDeletedFalse(getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found in database"));
    }

    private boolean hasRole(Authentication auth, Role role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role.name()));
    }

    public boolean isAdmin(){
        return hasRole(getAuthenticatedUser(), Role.ROLE_ADMIN);
    }

    public boolean isPlayer() {
        return hasRole(getAuthenticatedUser(), Role.ROLE_PLAYER);
    }

    public boolean isStadiumOwner(UUID stadiumId) {
        Authentication auth = getAuthenticatedUser();
        if (hasRole(auth, Role.ROLE_ADMIN)) return true;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return stadiumRepository.existsByIdAndOwner_Id(stadiumId, userDetails.getId());
        }
        throw new IllegalStateException("Could not retrieve user ID.");
    }

    public void checkOwnership(UUID stadiumId) {
        if (!isStadiumOwner(stadiumId)) {
            throw new AccessDeniedException("Permission denied. The stadium is not owned by the current manager.");
        }
    }

    public void checkBookingOwnership(UUID bookingUserId){
        if (!getCurrentUserId().equals(bookingUserId)) {
            throw new AccessDeniedException("Permission denied. You are not the owner of this booking.");
        }
    }

    private Authentication getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated.");
        }
        return authentication;
    }
}