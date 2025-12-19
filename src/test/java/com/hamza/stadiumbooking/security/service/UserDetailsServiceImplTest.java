package com.hamza.stadiumbooking.security.service;

import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    // --- Test Data ---
    private User sharedUser;
    private final String sharedEmail = "hamzasaleh@gmail.com";

    @BeforeEach
    void setup() {
        sharedUser = new User(1L, 0L, "Hamza Saleh", sharedEmail, "01234567890",
                "hashedPassword", LocalDate.of(2001, 2, 15), Role.ROLE_PLAYER, null, false
        );
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetailsWhenUserExists() {
        given(userRepository.findByEmailAndIsDeletedFalse(sharedEmail)).willReturn(Optional.of(sharedUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(sharedEmail);

        verify(userRepository, times(1)).findByEmailAndIsDeletedFalse(sharedEmail);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(sharedEmail);
        assertThat(userDetails.getPassword()).isEqualTo(sharedUser.getPassword());

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains(sharedUser.getRole().name());
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        String nonExistentEmail = "ghost@example.com";
        given(userRepository.findByEmailAndIsDeletedFalse(nonExistentEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(nonExistentEmail))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: " + nonExistentEmail);

        verify(userRepository, times(1)).findByEmailAndIsDeletedFalse(nonExistentEmail);
    }
}