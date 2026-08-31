package com.hamza.stadiumbooking.user;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapUserToResponse() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Hamza")
                .email("hamza@gmail.com")
                .phoneNumber("01012345678")
                .role(Role.ROLE_ADMIN)
                .dob(LocalDate.of(1995, 1, 1))
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertEquals(user.getId(), response.id());
        assertEquals("Hamza", response.name());
        assertEquals("hamza@gmail.com", response.email());
        assertEquals(Role.ROLE_ADMIN, response.role());
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        User user = User.builder()
                .name("Old Name")
                .email("old@gmail.com")
                .phoneNumber("01000000000")
                .password("oldPassword")
                .dob(LocalDate.of(1990, 1, 1))
                .role(Role.ROLE_PLAYER)
                .build();

        UserUpdateRequest request = new UserUpdateRequest(
                "New Name",
                "new@gmail.com",
                "01099999999",
                null,
                LocalDate.of(2000, 2, 2)
        );

        userMapper.updateUserFromRequest(request, user);

        assertEquals("New Name", user.getName());
        assertEquals("new@gmail.com", user.getEmail());
        assertEquals("01099999999", user.getPhoneNumber());
        assertEquals(LocalDate.of(2000, 2, 2), user.getDob());
        assertEquals("oldPassword", user.getPassword());
        assertEquals(Role.ROLE_PLAYER, user.getRole());
    }
}
