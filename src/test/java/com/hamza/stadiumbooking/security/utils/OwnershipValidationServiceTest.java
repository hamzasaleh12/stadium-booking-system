package com.hamza.stadiumbooking.security.utils;

import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.security.service.CustomUserDetails;
import com.hamza.stadiumbooking.stadium.StadiumRepository;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OwnershipValidationServiceTest {
   @InjectMocks
   private OwnershipValidationService ownershipValidationService;
   @Mock
   private UserRepository userRepository;
   @Mock
   private StadiumRepository stadiumRepository;

    private User player;
    private UUID sharedPlayerId;
    private UUID sharedStadiumId;
    private UUID sharedManagerId;
    private Authentication authentication;
    private CustomUserDetails customUserDetailsForPlayer;
    private CustomUserDetails customUserDetailsForManager;
    private CustomUserDetails customUserDetailsForAdmin;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        sharedPlayerId = UUID.randomUUID();
        sharedManagerId = UUID.randomUUID();
        UUID sharedAdminId = UUID.randomUUID();

        player = new User(
                sharedPlayerId, 0L, "Player Name", "player@example.com", "01000000000",
                "pass", LocalDate.of(2000, 5, 20), null, null, Role.ROLE_PLAYER, false
        );
        sharedManagerId = UUID.randomUUID();
        User manager = new User(
                sharedManagerId, 0L, "Manager Name", "manager@example.com", "01111111111",
                "pass", LocalDate.of(1990, 1, 1), null, null, Role.ROLE_MANAGER, false
        );
        sharedStadiumId = UUID.randomUUID();

        customUserDetailsForPlayer = new CustomUserDetails(
                sharedPlayerId, "player@gmail.com", "", false,
                List.of(new SimpleGrantedAuthority(Role.ROLE_PLAYER.name()))
        );

        customUserDetailsForManager = new CustomUserDetails(
                sharedManagerId, "manager@gmail.com", "", false,
                List.of(new SimpleGrantedAuthority(Role.ROLE_MANAGER.name()))
        );

        customUserDetailsForAdmin = new CustomUserDetails(
                sharedAdminId, "admin@gmail.com", "", false,
                List.of(new SimpleGrantedAuthority(Role.ROLE_ADMIN.name()))
        );
    }

    @Test
    void getCurrentUserEmail() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getName()).willReturn("hamza@gmail.com");

        String currentUserEmail = ownershipValidationService.getCurrentUserEmail();

        assertThat(currentUserEmail).isNotNull();
        assertThat(currentUserEmail).isEqualTo("hamza@gmail.com");
    }
    @Test
    void getCurrentUserEmail_shouldThrowIllegalStateException_WhenAuthenticationIsNull() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(null);

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUserEmail()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated.");
    }
    @Test
    void getCurrentUserEmail_shouldThrowIllegalStateException_WhenNotAuthenticated() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUserEmail()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated.");
    }

    @Test
    void getCurrentUserId() {
        mockSecurityContextWith(customUserDetailsForPlayer);

        UUID currentUserId = ownershipValidationService.getCurrentUserId();

        assertThat(currentUserId).isEqualTo(customUserDetailsForPlayer.getId());
    }
    @Test
    void getCurrentUserId_shouldThrowResourceNotFoundException() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(null);

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUserId()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not retrieve user ID from security context.");
    }
    @Test
    void getCurrentUserId_shouldThrowIllegalStateException_WhenAuthenticationIsNull() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(null);

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUserId()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated.");
    }
    @Test
    void getCurrentUserId_shouldThrowIllegalStateException_WhenNotAuthenticated() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(false);

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUserId()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not authenticated.");
    }

    @Test
    void getCurrentUser_forPlayer() {
        mockSecurityContextWith(customUserDetailsForPlayer);
        UUID currentUserId = ownershipValidationService.getCurrentUserId();
        given(userRepository.findByIdAndIsDeletedFalse(currentUserId)).willReturn(Optional.of(player));

        User currentUser = ownershipValidationService.getCurrentUser();

        assertThat(currentUser.getEmail()).isEqualTo(player.getEmail());
        assertThat(currentUser.getId()).isEqualTo(sharedPlayerId);
    }
    @Test
    void getCurrentUser_forPlayer_shouldThrowUserNotFound() {
        mockSecurityContextWith(customUserDetailsForPlayer);
        UUID currentUserId = ownershipValidationService.getCurrentUserId();
        given(userRepository.findByIdAndIsDeletedFalse(currentUserId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ownershipValidationService.getCurrentUser()).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Current user not found in database");
    }

    @Test
    void isAdmin_whenTheRolesIsPlayer() {
        mockSecurityContextLight(customUserDetailsForPlayer);

        boolean isAdmin = ownershipValidationService.isAdmin();

        assertThat(isAdmin).isFalse();
    }
    @Test
    void isAdmin_whenTheRolesIsManager() {
        mockSecurityContextLight(customUserDetailsForManager);

        boolean isAdmin = ownershipValidationService.isAdmin();

        assertThat(isAdmin).isFalse();
    }
    @Test
    void isAdmin_whenTheRolesIsAdmin() {
        mockSecurityContextLight(customUserDetailsForAdmin);

        boolean isAdmin = ownershipValidationService.isAdmin();

        assertThat(isAdmin).isTrue();
    }

    @Test
    void isPlayer_whenTheRolesIsPlayer() {
        mockSecurityContextLight(customUserDetailsForPlayer);

        boolean isAdmin = ownershipValidationService.isPlayer();

        assertThat(isAdmin).isTrue();
    }
    @Test
    void isPlayer_whenTheRolesIsManager() {
        mockSecurityContextLight(customUserDetailsForManager);

        boolean isAdmin = ownershipValidationService.isPlayer();

        assertThat(isAdmin).isFalse();
    }
    @Test
    void isPlayer_whenTheRolesIsAdmin() {
        mockSecurityContextLight(customUserDetailsForAdmin);

        boolean isAdmin = ownershipValidationService.isPlayer();

        assertThat(isAdmin).isFalse();
    }

    @Test
    void isStadiumOwner_whenTheRolesIsPlayer() {
        mockSecurityContextWith(customUserDetailsForPlayer);
        given(stadiumRepository.existsByIdAndOwner_Id(sharedStadiumId, sharedPlayerId))
                .willReturn(false);

        boolean isStadiumOwner = ownershipValidationService.isStadiumOwner(sharedStadiumId);

        assertThat(isStadiumOwner).isFalse();
    }
    @Test
    void isStadiumOwner_whenTheRolesIsManager() {
        mockSecurityContextWith(customUserDetailsForManager);
        given(stadiumRepository.existsByIdAndOwner_Id(sharedStadiumId, sharedManagerId))
                .willReturn(true);

        boolean isStadiumOwner = ownershipValidationService.isStadiumOwner(sharedStadiumId);

        assertThat(isStadiumOwner).isTrue();
    }
    @Test
    void isStadiumOwner_whenTheRolesIsManager2() {
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(player);

        assertThatThrownBy(() -> ownershipValidationService.isStadiumOwner(sharedStadiumId)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not retrieve user ID.");
    }
    @Test
    void isStadiumOwner_whenTheRolesIsAdmin() {
        mockSecurityContextLight(customUserDetailsForAdmin);

        boolean isStadiumOwner = ownershipValidationService.isStadiumOwner(sharedStadiumId);

        assertThat(isStadiumOwner).isTrue();
    }

    @Test
    void checkOwnership_shouldThrowAccessDenied_whenNotStadiumOwner() {
        mockSecurityContextWith(customUserDetailsForPlayer);
        given(stadiumRepository.existsByIdAndOwner_Id(sharedStadiumId, sharedPlayerId))
                .willReturn(false);

        assertThatThrownBy(() -> ownershipValidationService.checkOwnership(sharedStadiumId)).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Permission denied. The stadium is not owned by the current manager.");
    }
    @Test
    void checkOwnership_shouldSucceed() {
        mockSecurityContextWith(customUserDetailsForPlayer);
        given(stadiumRepository.existsByIdAndOwner_Id(sharedStadiumId, sharedPlayerId))
                .willReturn(true);

        assertDoesNotThrow(() -> ownershipValidationService.checkOwnership(sharedStadiumId));
    }

    @Test
    void checkBookingOwnership() {
        mockSecurityContextWith(customUserDetailsForPlayer);

        assertDoesNotThrow(() -> ownershipValidationService.checkBookingOwnership(sharedPlayerId));
    }
    @Test
    void checkBookingOwnership_shouldThrowAccessDeniedException_whenUserIsNotTheOwner() {
        mockSecurityContextWith(customUserDetailsForPlayer);

        assertThatThrownBy(() -> ownershipValidationService.checkBookingOwnership(UUID.randomUUID())).isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Permission denied. You are not the owner of this booking.");
    }

    public void mockSecurityContextWith(CustomUserDetails user){
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);
        given(authentication.getPrincipal()).willReturn(user);
    }
    public void mockSecurityContextLight(CustomUserDetails user){
        given(SecurityContextHolder.getContext().getAuthentication()).willReturn(authentication);
        given(authentication.isAuthenticated()).willReturn(true);

        org.mockito.Mockito.doReturn(user.getAuthorities()).when(authentication).getAuthorities();
    }
}