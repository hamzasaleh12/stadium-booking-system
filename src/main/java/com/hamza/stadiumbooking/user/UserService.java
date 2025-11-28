package com.hamza.stadiumbooking.user;

import com.hamza.stadiumbooking.exception.EmailTakenException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User not found with ID: " + id));
        return mapToDto(user);
    }

    public UserResponse addUser(UserRequest userRequest) {
        if (userRepository.findByEmail(userRequest.email()).isPresent()) {
            throw new EmailTakenException("Email is already taken: " + userRequest.email());
        }
        User newUser = mapToEntity(userRequest);
        newUser.setRole(Role.PLAYER);
        User savedUser = userRepository.save(newUser);
        return mapToDto(savedUser);
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setName(request.name() != null ? request.name() : user.getName());

        // تعديل الشرط التاني عشان في الابديت ميعملش ايرور بسبب تكرار الايميل لنفس اليوزر
        if (request.email() != null && !request.email().isEmpty()) {
            if (!request.email().equals(user.getEmail())) {
                Optional<User> userWithSameEmail = userRepository.findByEmail(request.email());
                if (userWithSameEmail.isPresent()) {
                    throw new EmailTakenException("Email " + request.email() + " is already taken.");
                }
                user.setEmail(request.email());
            }
        }

        if (request.phoneNumber() != null && request.phoneNumber().length() == 11) {
            user.setPhoneNumber(request.phoneNumber());
        }

        if (request.password() != null && !request.password().isEmpty()) {
            user.setPassword(request.password());
        }

        if (request.dob() != null) {
            user.setDob(request.dob());
        }

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    private UserResponse mapToDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole()
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
