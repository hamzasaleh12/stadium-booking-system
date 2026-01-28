package com.hamza.stadiumbooking.user;

import com.hamza.stadiumbooking.exception.EmailTakenException;
import com.hamza.stadiumbooking.exception.PhoneNumberTakenException;
import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;

    // --- Shared Test Data ---
    private final UUID sharedUserId = UUID.randomUUID();
    private final String sharedPassword = "1111";
    private final String sharedEmail = "hamzasaleh@gmail.com";
    private final String sharedPhoneNumber = "01234567890";
    private final String sharedName = "Hamza Saleh";
    private User sharedOriginalUser;
    private User sharedUserCopy;

    private Pageable pageable;


    @BeforeEach
    void setup() {
        sharedOriginalUser = new User(
                sharedUserId,0L,sharedName , sharedEmail,sharedPhoneNumber ,
                sharedPassword, LocalDate.of(2001, 2, 15), null,null, Role.ROLE_PLAYER,false
        );
        sharedUserCopy = new User(
                sharedOriginalUser.getId(), 0L,sharedOriginalUser.getName(), sharedOriginalUser.getEmail(),
                sharedOriginalUser.getPhoneNumber(), sharedOriginalUser.getPassword(),
                sharedOriginalUser.getDob(), null,null,sharedOriginalUser.getRole(),false
        );
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        User user1 = new User(UUID.randomUUID(), 0L,"Hamza", "h@g.com", "010", "123", LocalDate.of(2000, 1, 1), null,null,Role.ROLE_PLAYER, false);
        User user2 = new User(UUID.randomUUID(), 0L,"Ali", "a@g.com", "011", "456", LocalDate.of(1995, 5, 5), null,null,Role.ROLE_PLAYER, false);

        List<User> users = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(users);

        given(userRepository.findAllByIsDeletedFalse(pageable)).willReturn(userPage);

        Page<UserResponse> responses = userService.getAllUsers(pageable);

        verify(userRepository, times(1)).findAllByIsDeletedFalse(pageable);

        assertThat(responses.getContent())
                .extracting(UserResponse::id)
                .containsExactlyInAnyOrder(user1.getId(), user2.getId());
    }
    @Test
    void getAllUsers_ShouldReturnEmpty() {
        given(userRepository.findAllByIsDeletedFalse(pageable)).willReturn(Page.empty());

        Page<UserResponse> responses =userService.getAllUsers(pageable);

        verify(userRepository, times(1)).findAllByIsDeletedFalse(pageable);
        assertThat(responses).isEmpty();
    }


    @Test
    void getUserById_ShouldReturnUserWhenFound() {
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.of(sharedUserCopy));

        UserResponse response = userService.getUserById(sharedUserId);

        verify(userRepository,times(1)).findByIdAndIsDeletedFalse(sharedUserId);
        assertThat(response.id()).isEqualTo(sharedUserId);
        assertThat(response.name()).isEqualTo(sharedName);
    }
    @Test
    void shouldThrowResourceNotFoundException_ForGetUser() {
        UUID nonExistentId = UUID.randomUUID();
        given(userRepository.findByIdAndIsDeletedFalse(nonExistentId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: " + nonExistentId);
    }

    @Test
    void shouldGetUserIdByEmail(){
        given(userRepository.findByEmailAndIsDeletedFalse(sharedEmail)).willReturn(Optional.of(sharedUserCopy));
        UUID id = userService.getUserIdByEmail(sharedEmail);

        assertThat(id).isEqualTo(sharedUserId);
    }
    @Test
    void shouldNotGetUserIdByEmail(){
        given(userRepository.findByEmailAndIsDeletedFalse(sharedEmail)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserIdByEmail(sharedEmail)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found for email: " + sharedEmail);
        verify(userRepository, times(1)).findByEmailAndIsDeletedFalse(sharedEmail);
    }

    @Test
    void addUser_ShouldSaveNewUser() {
        UserRequest userRequest = new UserRequest(
                sharedName, "player@gmail.com", sharedPhoneNumber, sharedPassword,
                LocalDate.of(2000, 1, 1)
        );

        UUID expectedId = UUID.randomUUID();
        User savedUser = new User(expectedId, 0L, sharedName, userRequest.email(), sharedPhoneNumber, sharedPassword,
                userRequest.dob(), null,null,Role.ROLE_PLAYER, false);

        given(userRepository.findByEmailAndIsDeletedFalse(userRequest.email())).willReturn(Optional.empty());
        given(userRepository.existsByPhoneNumberAndIsDeletedFalse(userRequest.phoneNumber())).willReturn(false);
        given(passwordEncoder.encode(userRequest.password())).willReturn("hashed_password");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        UserResponse response = userService.addUser(userRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();

        assertThat(capturedUser.getName()).isEqualTo(sharedName);
        assertThat(capturedUser.getEmail()).isEqualTo("player@gmail.com");
        assertThat(capturedUser.getPassword()).isEqualTo("hashed_password");

        assertThat(response.id()).isEqualTo(savedUser.getId());
        assertThat(response.name()).isEqualTo(sharedName);
    }
    @Test
    void addUser_ShouldThrowEmailTakenException() {
        UserRequest userRequest = new UserRequest(
                sharedName, sharedEmail, sharedPhoneNumber, sharedPassword,
                LocalDate.of(2000, 1, 1)
        );
        given(userRepository.findByEmailAndIsDeletedFalse(userRequest.email())).willReturn(Optional.of(sharedOriginalUser));

        assertThatThrownBy(() -> userService.addUser(userRequest)).isInstanceOf(EmailTakenException.class)
                .hasMessageContaining("Email is already taken: " + userRequest.email());

        verify(userRepository, never()).save(any());
    }
    @Test
    void addUser_ShouldThrowPhoneTakenException() {
        UserRequest userRequest = new UserRequest(
                sharedName, sharedEmail, sharedPhoneNumber, sharedPassword,
                LocalDate.of(2000, 1, 1)
        );
        given(userRepository.findByEmailAndIsDeletedFalse(userRequest.email())).willReturn(Optional.empty());
        given(userRepository.existsByPhoneNumberAndIsDeletedFalse(userRequest.phoneNumber())).willReturn(true);

        assertThatThrownBy(() -> userService.addUser(userRequest)).isInstanceOf(PhoneNumberTakenException.class)
                .hasMessageContaining("Phone Number is already taken: " + userRequest.phoneNumber());

        verify(userRepository, never()).save(any());
    }
    @Test
    void addUser_ShouldThrowIllegalArgumentExceptionWhenAgeIsLessThan5() {
        UserRequest userRequest = new UserRequest(
                sharedName, sharedEmail, sharedPhoneNumber, sharedPassword,
                LocalDate.of(2026, 1, 1)
        );
        given(userRepository.findByEmailAndIsDeletedFalse(userRequest.email())).willReturn(Optional.empty());
        given(userRepository.existsByPhoneNumberAndIsDeletedFalse(userRequest.phoneNumber())).willReturn(false);

        assertThatThrownBy(() -> userService.addUser(userRequest)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The age must be at least 5 years to register.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser() {
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.of(sharedUserCopy));
        given(userRepository.save(sharedUserCopy)).willReturn(sharedUserCopy);

        userService.deleteUser(sharedUserId);

        verify(userRepository, times(1)).findByIdAndIsDeletedFalse(sharedUserId);
        verify(userRepository,times(1)).save(sharedUserCopy);
        assertThat(sharedUserCopy.isDeleted()).isEqualTo(true);
        assertThat(sharedUserCopy.getEmail())
                .startsWith("deleted_")
                .contains(sharedEmail)
                .isNotEqualTo(sharedEmail);

        assertThat(sharedUserCopy.getPhoneNumber())
                .startsWith("del_")
                .isNotEqualTo(sharedPhoneNumber);
    }
    @Test
    void deleteUser_ShouldNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        given(userRepository.findByIdAndIsDeletedFalse(nonExistentId)).willReturn(Optional.empty());
        assertThatThrownBy(() -> userService.deleteUser(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: " + nonExistentId);

        verify(userRepository, times(1)).findByIdAndIsDeletedFalse(nonExistentId);
    }

    @Test
    void updateUser_ShouldSucceed_WhenAllIsUpdated() {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(
                "Hamza Updated", "hamza_new@gmail.com",
                "01122334455","123qwe", LocalDate.of(2003,12,3)
        );
        setupUserCrudMocks();
        given(passwordEncoder.encode(request.password())).willReturn("hashed_password");

        userService.updateUser(sharedUserId, request);

        assertThat(sharedUserCopy.getName()).isEqualTo(request.name());
        assertThat(sharedUserCopy.getEmail()).isEqualTo(request.email());
        assertThat(sharedUserCopy.getPassword()).isEqualTo("hashed_password");
        assertThat(sharedUserCopy.getDob()).isEqualTo(request.dob());

        verify(userRepository).save(sharedUserCopy);
    }
    @Test
    void updateUser_ShouldThrowResourceNotFoundException() {
        UserUpdateRequest request = new UserUpdateRequest(
                "New Name", "test@email.com", "01234567890", "pass", null
        );
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(sharedUserId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: " + sharedUserId);

        verify(userRepository, never()).save(any());
    }
    @Test
    void updateUser_ShouldThrowEmailTakenException_WhenNewEmailIsTakenByAnotherUser() {
        String takenEmail = "taken.by.99@gmail.com";
        User userWithTakenEmail = new User(
                UUID.randomUUID(),0L, "Taken User", takenEmail, "9999",
                "555", LocalDate.of(2000, 1, 1), null,null,Role.ROLE_PLAYER,false
        );

        UserUpdateRequest userRequest = new UserUpdateRequest(
                sharedUserCopy.getName(),
                takenEmail, // The email being checked
                sharedUserCopy.getPhoneNumber(), sharedUserCopy.getPassword(),
                sharedUserCopy.getDob()
        );

        given(userRepository.findByIdAndIsDeletedFalse(sharedUserCopy.getId())).willReturn(Optional.of(sharedUserCopy));
        given(userRepository.findByEmailAndIsDeletedFalse(takenEmail)).willReturn(Optional.of(userWithTakenEmail));

        assertThatThrownBy(() -> userService.updateUser(sharedUserCopy.getId(), userRequest))
                .isInstanceOf(EmailTakenException.class)
                .hasMessageContaining("Email " + userRequest.email() + " is already taken.");

        // Verify only checks happened, no save
        verify(userRepository, never()).save(any());
        verify(userRepository, times(1)).findByEmailAndIsDeletedFalse(takenEmail);
    }
    @Test
    void updateUser_ShouldThrowPhoneTakenException_WhenNewPhoneIsTakenByAnotherUser() {
        String takenPhone = "01234567891";
        UserUpdateRequest userRequest = new UserUpdateRequest(
                sharedUserCopy.getName(),
                sharedUserCopy.getEmail(),
                takenPhone,
                sharedUserCopy.getPassword(),
                sharedUserCopy.getDob()
        );

        given(userRepository.findByIdAndIsDeletedFalse(sharedUserCopy.getId())).willReturn(Optional.of(sharedUserCopy));
        given(userRepository.existsByPhoneNumberAndIsDeletedFalse(userRequest.phoneNumber())).willReturn(true);

        assertThatThrownBy(() -> userService.updateUser(sharedUserCopy.getId(), userRequest))
                .isInstanceOf(PhoneNumberTakenException.class)
                .hasMessageContaining("Phone number " + userRequest.phoneNumber() + " is already taken.");

        // Verify only checks happened, no save
        verify(userRepository, never()).save(any());
        verify(userRepository,times(1)).existsByPhoneNumberAndIsDeletedFalse(takenPhone);
    }
    @Test
    void updateUser_ShouldThrowAgeException_WhenAgeIsLessThan5() {
        UserUpdateRequest userRequest = new UserUpdateRequest(
                sharedUserCopy.getName(),
                sharedUserCopy.getEmail(),
                sharedUserCopy.getPhoneNumber(),
                sharedUserCopy.getPassword(),
                LocalDate.of(2026,1,1)
        );

        given(userRepository.findByIdAndIsDeletedFalse(sharedUserCopy.getId())).willReturn(Optional.of(sharedUserCopy));

        assertThatThrownBy(() -> userService.updateUser(sharedUserCopy.getId(), userRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Age must be at least 5 years");

        // Verify only checks happened, no save
        verify(userRepository, never()).save(any());
    }
    @Test
    void updateUser_ShouldKeepOldData_WhenNewValuesAreNullOrInvalid() {
        UserUpdateRequest invalidRequest = new UserUpdateRequest(
                "",
                null,
                "0123",
                "",
                null
        );
        setupUserCrudMocks();
        userService.updateUser(sharedUserId, invalidRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User updatedUser = captor.getValue();

        assertThat(updatedUser.getName()).isEqualTo(sharedOriginalUser.getName());
        assertThat(updatedUser.getEmail()).isEqualTo(sharedOriginalUser.getEmail());
        assertThat(updatedUser.getPassword()).isEqualTo(sharedOriginalUser.getPassword());
        assertThat(updatedUser.getDob()).isEqualTo(sharedOriginalUser.getDob());

        verify(userRepository, never()).findByEmailAndIsDeletedFalse(anyString());
    }
    @Test
    void updateUser_ShouldHandleEmailLogicCorrectly() {
        UserUpdateRequest sameEmailAndPhoneRequest = new UserUpdateRequest(null, sharedEmail, sharedPhoneNumber, null, null);
        setupUserCrudMocks();

        userService.updateUser(sharedUserId, sameEmailAndPhoneRequest);

        verify(userRepository, never()).findByEmailAndIsDeletedFalse(anyString());
        verify(userRepository, never()).existsByPhoneNumberAndIsDeletedFalse(anyString());

        UserUpdateRequest conflictRequest = new UserUpdateRequest(null, "", null, null, null);
        setupUserCrudMocks();
        UserResponse response = userService.updateUser(sharedUserId, conflictRequest);

        verify(userRepository, never()).findByEmailAndIsDeletedFalse(anyString());
        assertThat(response.email()).isEqualTo(sharedEmail);
    }

    @Test
    void changeUserRole(){
        setupUserCrudMocks();

        userService.changeUserRole(sharedUserId, String.valueOf(Role.ROLE_MANAGER));

        assertThat(sharedUserCopy.getRole()).isEqualTo(Role.ROLE_MANAGER);
        verify(userRepository).save(sharedUserCopy);
    }
    @Test
    void changeUserRole_ShouldNotFound(){
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeUserRole(sharedUserId, String.valueOf(Role.ROLE_MANAGER))).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID: " + sharedUserId);
    }
    @Test
    void changeUserRole_ShouldThrowIllegalArgumentException(){
        String fakeRole = "SUPER_ADMIN";
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.of(sharedUserCopy));

        assertThatThrownBy(() -> userService.changeUserRole(sharedUserId, fakeRole)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Role: " + fakeRole);
        verify(userRepository,never()).save(sharedUserCopy);
    }

    private void setupUserCrudMocks() {
        given(userRepository.findByIdAndIsDeletedFalse(sharedUserId)).willReturn(Optional.of(sharedUserCopy));
        given(userRepository.save(any(User.class))).willReturn(sharedUserCopy);
    }
}