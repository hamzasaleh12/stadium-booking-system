package com.hamza.stadiumbooking.user;

import com.hamza.stadiumbooking.exception.EmailTakenException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;


@Service @Slf4j @RequiredArgsConstructor
public class UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Action: getAllUsers | Fetching users from DB (Page: {})", pageable.getPageNumber());
        return userRepository.findAllByIsDeletedFalse(pageable).map(this::mapToDto);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndIsDeletedFalse(id).orElseThrow(
                () -> {
                    log.error("Action: getUserById | Failure | User ID {} not found", id);
                    return new ResourceNotFoundException("User not found with ID: " + id);
                });
        return mapToDto(user);
    }

    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> {
                    log.error("Action: getUserIdByEmail | Failure | No user found with email: {}", email);
                    return new ResourceNotFoundException("User not found for email: " + email);
                });
        return user.getId();
    }

    @Transactional
    public UserResponse addUser(UserRequest userRequest) {
        boolean isPresent = userRepository.findByEmailAndIsDeletedFalse(userRequest.email()).isPresent();
        if (isPresent) {
            log.warn("Action: addUser | Failure | Email already exists: {}", userRequest.email());
            throw new EmailTakenException("Email is already taken: " + userRequest.email());
        }
        int age = Period.between(userRequest.dob(), LocalDate.now()).getYears();
        if (age < 5) {
            throw new IllegalArgumentException("the age must be at least 5 years to register.");
        }
        User newUser = mapToEntity(userRequest);
        newUser.setPassword(passwordEncoder.encode(userRequest.password()));
        User savedUser = userRepository.save(newUser);

        log.info("Action: addUser | Success | User registered with ID: {}", savedUser.getId());
        return mapToDto(savedUser);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId).orElseThrow(
                () -> {
                    log.error("Action: deleteUser | Failure | User ID {} not found", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        user.setDeleted(true);
        userRepository.save(user);
        log.info("Action: deleteUser | Success | User ID {} marked as deleted", userId);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.error("Action: updateUser | Failure | User ID {} not found", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        boolean passwordChanged = request.password() != null && !request.password().isEmpty();
        if (passwordChanged) {
            log.warn("Action: updateUser | Security Alert | Password changed for User ID: {}", userId);
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user.setName((request.name() != null && !request.name().isEmpty()) ? request.name() : user.getName());

        if (request.email() != null && !request.email().isEmpty()) {
            if (!request.email().equals(user.getEmail())) {

                Optional<User> userWithSameEmail = userRepository.findByEmailAndIsDeletedFalse(request.email());

                if (userWithSameEmail.isPresent()) {
                    log.warn("Action: updateUser | Conflict | Email {} is already taken", request.email());
                    throw new EmailTakenException("Email " + request.email() + " is already taken.");
                }
                user.setEmail(request.email());
            }
        }

        if (request.phoneNumber() != null && request.phoneNumber().length() == 11) {
            user.setPhoneNumber(request.phoneNumber());
        }

        if (request.dob() != null) {
            int age = Period.between(request.dob(), LocalDate.now()).getYears();
            if (age < 5) {
                log.warn("Action: updateUser | Validation Failed | Age {} is too young for User ID: {}", age, userId);
                throw new IllegalArgumentException("عفواً، السن لازم يكون 5 سنين على الأقل.");
            }
            user.setDob(request.dob());
        }

        User savedUser = userRepository.save(user);
        log.info("Action: updateUser | Success | User ID {} updated successfully", savedUser.getId());
        return mapToDto(savedUser);
    }

    public UserResponse changeUserRole(Long userId, String newRoleAsString) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> {
                    log.error("Action: changeUserRole | Failure | User ID {} not found", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        try {
            user.setRole(Role.valueOf(newRoleAsString.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.error("Action: changeUserRole | Failure | Invalid Role: {}", newRoleAsString);
            throw new IllegalArgumentException("Invalid Role: " + newRoleAsString);
        }

        User savedUser = userRepository.save(user);
        log.info("Action: changeUserRole | Success | User ID: {} role updated to {}", userId, savedUser.getRole());
        return mapToDto(savedUser);
    }

    // ... Helper Methods (mapToDto, mapToEntity) ...
    private UserResponse mapToDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getAge()
        );
    }

    private User mapToEntity(UserRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(request.password());
        user.setDob(request.dob());
        return user;
    }
}
