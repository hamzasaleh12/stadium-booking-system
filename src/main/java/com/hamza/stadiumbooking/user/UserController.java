package com.hamza.stadiumbooking.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @Slf4j @RequiredArgsConstructor
@RequestMapping(path = "/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(@ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Incoming request to get ALL Users | Page: {}", pageable.getPageNumber());
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PLAYER') and #userId == authentication.principal.id)")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        log.info("Incoming request to get User details for ID: {}", userId);
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PostMapping
    public ResponseEntity<UserResponse> addUser(@RequestBody @Valid UserRequest userRequest) {
        log.info("Incoming request to register new User with Email: '{}'", userRequest.email());
        UserResponse savedUser = userService.addUser(userRequest);

        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PLAYER') and #userId == authentication.principal.id)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        log.info("Incoming request to delete User ID: {}",userId);
        userService.deleteUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping(path = "/{userId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('PLAYER') and #userId == authentication.principal.id)")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserUpdateRequest userUpdateRequest
    ) {
        log.info("Incoming request to update User ID: {}", userId);
        UserResponse updatedUser = userService.updateUser(userId, userUpdateRequest);
        return ResponseEntity.ok().body(updatedUser);
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeUserRole(
            @PathVariable Long userId,
            @RequestParam String roleAsString
    ) {
        log.info("Incoming request to change role for User ID: {} to new Role: '{}'", userId, roleAsString);
        return ResponseEntity.ok().body(userService.changeUserRole(userId, roleAsString));
    }
}
