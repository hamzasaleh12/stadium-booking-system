package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StadiumServiceTest {
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private StadiumService stadiumService;
    @Mock
    private OwnershipValidationService ownershipValidationService;

    private final UUID sharedManagerId = UUID.randomUUID();
    private final UUID sharedStadiumId = UUID.randomUUID();
    private final Type sharedType = Type.ELEVEN_A_SIDE;

    private User manger;
    private Stadium sharedStadiumCopy;
    private Stadium sharedOriginalStadium;

    private List<Stadium> stadiums;
    private Page<Stadium> stadiumPage;

    @BeforeEach
    void setUp() {
        String sharedManagerEmail = "hamza@gmail.com";
        String sharedManagerName = "hamza";
        manger = new User(
                sharedManagerId, 0L, sharedManagerName, sharedManagerEmail, "01234567890",
                "12345", LocalDate.of(2001, 2, 15), null, null, Role.ROLE_MANAGER, false
        );

        String sharedStadiumName = "Al-Ahly";
        Double sharedPrice = 500.00;

        sharedOriginalStadium = new Stadium(
                sharedStadiumId, 0L, sharedStadiumName, "Nasr-city", sharedPrice, "image.com", sharedType, 50,
                LocalTime.of(10, 0), LocalTime.of(23, 0), null, new HashSet<>(Set.of("Wifi", "Parking")),
                manger, false, null, null
        );

        sharedStadiumCopy = new Stadium(
                sharedOriginalStadium.getId(), 0L, sharedOriginalStadium.getName(), sharedOriginalStadium.getLocation(), sharedOriginalStadium.getPricePerHour(),
                sharedOriginalStadium.getPhotoUrl(), sharedOriginalStadium.getType(), sharedOriginalStadium.getBallRentalFee(), sharedOriginalStadium.getOpenTime(), sharedOriginalStadium.getCloseTime(),
                null, sharedOriginalStadium.getFeatures(), sharedOriginalStadium.getOwner(), false, null, null
        );
        stadiums = List.of(sharedStadiumCopy);
        stadiumPage = new PageImpl<>(stadiums);
    }

    @Test
    void getAllStadiums() {
        Stadium secondStadium = new Stadium(
                UUID.randomUUID(), 0L, "Al-Ahly", "Nasr-city", 500.00, "image.com", Type.ELEVEN_A_SIDE,
                50, LocalTime.of(9, 0), LocalTime.of(22, 0), null, new HashSet<>(), manger, false, null, null
        );
        stadiums = Arrays.asList(sharedStadiumCopy, secondStadium);
        stadiumPage = new PageImpl<>(stadiums);
        given(stadiumRepository.findAllByIsDeletedFalse(Pageable.unpaged())).willReturn(stadiumPage);

        Page<StadiumResponse> responses = stadiumService.getAllStadiums(Pageable.unpaged());

        verify(stadiumRepository, times(1)).findAllByIsDeletedFalse(Pageable.unpaged());
        assertThat(responses.getContent()).extracting(StadiumResponse::id)
                .containsExactlyInAnyOrder(secondStadium.getId(), sharedStadiumId);
    }

    @Test
    void getAllStadiums_ShouldReturnEmpty() {
        given(stadiumRepository.findAllByIsDeletedFalse(Pageable.unpaged())).willReturn(Page.empty());

        Page<StadiumResponse> responses = stadiumService.getAllStadiums(Pageable.unpaged());

        verify(stadiumRepository, times(1)).findAllByIsDeletedFalse(Pageable.unpaged());
        assertThat(responses).isEmpty();
    }

    @Test
    void getAllLocations() {
        String location2 = "Doki";
        given(stadiumRepository.findAllDistinctLocations()).willReturn(List.of(sharedStadiumCopy.getLocation(), location2));

        List<String> responses = stadiumService.getAllLocations();

        verify(stadiumRepository, times(1)).findAllDistinctLocations();
        assertThat(responses).hasSize(2);
        assertThat(responses).containsExactlyInAnyOrder(location2, sharedStadiumCopy.getLocation());
    }

    @Test
    void getAllLocations_ShouldReturnEmpty() {
        given(stadiumRepository.findAllDistinctLocations()).willReturn(List.of());

        List<String> responses = stadiumService.getAllLocations();

        verify(stadiumRepository, times(1)).findAllDistinctLocations();
        assertThat(responses).isEmpty();
    }

    @Test
    void getStadiumById() {
        UUID id = sharedStadiumId;
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadiumCopy));

        StadiumResponse response = stadiumService.getStadiumById(id);

        verify(stadiumRepository, times(1)).findByIdAndIsDeletedFalse(sharedStadiumId);
        assertThat(id).isEqualTo(response.id());
    }

    @Test
    void getStadiumById_ShouldThrowResourceNotFoundIfNotFound() {
        UUID fakeId = UUID.randomUUID();
        given(stadiumRepository.findByIdAndIsDeletedFalse(fakeId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stadiumService.getStadiumById(fakeId)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium not found with ID: " + fakeId);

        verify(stadiumRepository, times(1)).findByIdAndIsDeletedFalse(fakeId);
    }

    @Test
    void findStadiumEntityById() {
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.ofNullable(sharedStadiumCopy));

        Stadium stadium = stadiumService.findStadiumEntityById(sharedStadiumId);

        assertThat(stadium).extracting(Stadium::getId).isEqualTo(sharedStadiumId);
    }

    @Test
    void findStadiumEntityById_ShouldReturnEmpty() {
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stadiumService.findStadiumEntityById(sharedStadiumId)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium linked to booking not found: " + sharedStadiumId);
    }

    @Test
    void addStadium() {
        StadiumRequest request = new StadiumRequest(
                "New Test Field", "Riyadh Location", 120.0, 10,
                LocalTime.of(8, 0), LocalTime.of(23, 0), Set.of("Showers"), Type.FIVE_A_SIDE, "https://new-photo.com"
        );

        given(ownershipValidationService.getCurrentUserId()).willReturn(manger.getId());
        given(userRepository.findByIdAndIsDeletedFalse(manger.getId())).willReturn(Optional.of(manger));

        Stadium savedEntity = new Stadium(
                UUID.randomUUID(), 0L, request.name(), request.location(), request.pricePerHour(),
                request.photoUrl(), request.type(), request.ballRentalFee(),
                request.openTime(), request.closeTime(), null, request.features(),
                manger, false, null, null
        );
        given(stadiumRepository.save(any(Stadium.class))).willReturn(savedEntity);

        StadiumResponse response = stadiumService.addStadium(request);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium capturedStadium = captor.getValue();

        assertThat(capturedStadium.getOwner()).isEqualTo(savedEntity.getOwner());
        assertThat(capturedStadium.getLocation()).isEqualTo(savedEntity.getLocation());
        assertThat(capturedStadium.getOpenTime()).isEqualTo(request.openTime());
        assertThat(capturedStadium.getFeatures()).contains("Showers");

        assertThat(capturedStadium.getId()).isNull();
        assertThat(response.id()).isEqualTo(savedEntity.getId());
    }

    @Test
    void addStadium_ShouldSetBallFeeToZeroIfNull() {
        UUID newStadiumId = UUID.randomUUID();
        StadiumRequest request = new StadiumRequest(
                "Null Fee Field", "Doha Location", 100.0, null,
                LocalTime.of(10, 0), LocalTime.of(22, 0), new HashSet<>(), Type.FIVE_A_SIDE, "https://null-fee.com"
        );
        given(ownershipValidationService.getCurrentUserId()).willReturn(manger.getId());
        given(userRepository.findByIdAndIsDeletedFalse(sharedManagerId)).willReturn(Optional.of(manger));

        Stadium savedEntity = new Stadium(
                newStadiumId, 0L, request.name(), request.location(), request.pricePerHour(),
                request.photoUrl(), request.type(), 0,
                request.openTime(), request.closeTime(), null, request.features(),
                manger, false, null, null
        );
        given(stadiumRepository.save(any(Stadium.class))).willReturn(savedEntity);

        stadiumService.addStadium(request);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium capturedStadium = captor.getValue();

        assertThat(capturedStadium.getBallRentalFee()).isEqualTo(0);
    }
    @Test
    void addStadiumThrowUserNotFound() {
        StadiumRequest request = new StadiumRequest(
                "New Test Field", "Riyadh Location", 120.0, 10,
                LocalTime.of(10, 0), LocalTime.of(22, 0), new HashSet<>(), Type.FIVE_A_SIDE, "https://new-photo.com"
        );
        given(ownershipValidationService.getCurrentUserId()).willReturn(manger.getId());
        given(userRepository.findByIdAndIsDeletedFalse(manger.getId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> stadiumService.addStadium(request)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Manager not found with ID: " + manger.getId());
    }
    @Test
    void addStadiumThrowAuthenticationAlert() {
        StadiumRequest request = new StadiumRequest(
                "New Test Field", "Riyadh Location", 120.0, 10,
                LocalTime.of(10, 0), LocalTime.of(22, 0), new HashSet<>(), Type.FIVE_A_SIDE, "https://new-photo.com"
        );
        given(ownershipValidationService.getCurrentUserId()).willReturn(null);

        assertThatThrownBy(() -> stadiumService.addStadium(request)).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Action: addStadium | Security Failure | User must be authenticated to add a stadium");

        verify(ownershipValidationService,times(1)).getCurrentUserId();
        verify(stadiumRepository,never()).save(any(Stadium.class));
    }

    @Test
    void deleteStadium() {
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadiumCopy));
        given(stadiumRepository.save(any(Stadium.class))).willReturn(sharedStadiumCopy);

        stadiumService.deleteStadium(sharedStadiumId);

        verify(stadiumRepository, times(1)).findByIdAndIsDeletedFalse(sharedStadiumId);
        verify(stadiumRepository, times(1)).save(sharedStadiumCopy);
        assertThat(sharedStadiumCopy.isDeleted()).isEqualTo(true);
    }

    @Test
    void deleteStadiumThrowStadiumNotFound() {
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stadiumService.deleteStadium(sharedStadiumId)).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium not found with ID: " + sharedStadiumId);
    }

    @Test
    void updateStadium() {
        StadiumRequestForUpdate newRequest = new StadiumRequestForUpdate(
                "New Test Name", 1500.0, 100, LocalTime.of(12, 0)
                , LocalTime.of(23, 59), Set.of("WiFi"), "new_photo_url.com"
        );
        setUpStadiumMocks();

        stadiumService.updateStadium(sharedStadiumId, newRequest);

        verify(stadiumRepository, times(1)).findByIdAndIsDeletedFalse(sharedStadiumId);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium updatedStadium = captor.getValue();

        assertThat(updatedStadium.getName()).isEqualTo(newRequest.name());
        assertThat(updatedStadium.getPricePerHour()).isEqualTo(newRequest.pricePerHour());
        assertThat(updatedStadium.getOpenTime()).isEqualTo(newRequest.openTime());
        assertThat(updatedStadium.getPhotoUrl()).isEqualTo(newRequest.photoUrl());
    }

    @Test
    void updateStadium_updateStadium_ShouldThrowNotFound_WhenStadiumDoesNotExist() {
        StadiumRequestForUpdate request = new StadiumRequestForUpdate(
                "Hacker Attempt", 99.0, 9,
                null, null, Set.of("WiFi"), "hacked_url"
        );
        assertThatThrownBy(() -> stadiumService.updateStadium(sharedStadiumId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Stadium not found with ID: " + sharedStadiumId);
    }

    @Test
    void updateStadium_ShouldKeepOldData_WhenNewValuesAreNullOrInvalid() {
        StadiumRequestForUpdate invalidRequest = new StadiumRequestForUpdate(
                null, -50.0, -10, null, null, null, ""
        );
        setUpStadiumMocks();

        stadiumService.updateStadium(sharedStadiumId, invalidRequest);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium updatedStadium = captor.getValue();

        assertThat(updatedStadium.getName()).isEqualTo(sharedOriginalStadium.getName());
        assertThat(updatedStadium.getPricePerHour()).isEqualTo(500.0);
        assertThat(updatedStadium.getBallRentalFee()).isEqualTo(50);
        assertThat(updatedStadium.getPhotoUrl()).isEqualTo(sharedOriginalStadium.getPhotoUrl());

        assertThat(updatedStadium.getOpenTime()).isEqualTo(sharedOriginalStadium.getOpenTime());
        assertThat(updatedStadium.getCloseTime()).isEqualTo(sharedOriginalStadium.getCloseTime());

        assertThat(updatedStadium.getLocation()).isEqualTo(sharedOriginalStadium.getLocation());
        assertThat(updatedStadium.getType()).isEqualTo(sharedOriginalStadium.getType());
    }

    @Test
    void updateStadium_ShouldKeepOldData_WhenValuesAreNullOrEmpty() {
        StadiumRequestForUpdate request = new StadiumRequestForUpdate(
                "", null, null, null, null, null, null
        );
        setUpStadiumMocks();

        stadiumService.updateStadium(sharedStadiumId, request);

        ArgumentCaptor<Stadium> captor = ArgumentCaptor.forClass(Stadium.class);
        verify(stadiumRepository).save(captor.capture());
        Stadium updatedStadium = captor.getValue();

        assertThat(updatedStadium.getName()).isEqualTo(sharedOriginalStadium.getName());
        assertThat(updatedStadium.getPricePerHour()).isEqualTo(sharedOriginalStadium.getPricePerHour());
        assertThat(updatedStadium.getBallRentalFee()).isEqualTo(sharedOriginalStadium.getBallRentalFee());
        assertThat(updatedStadium.getOpenTime()).isEqualTo(sharedOriginalStadium.getOpenTime());
    }

    private void setUpStadiumMocks() {
        given(stadiumRepository.findByIdAndIsDeletedFalse(sharedStadiumId)).willReturn(Optional.of(sharedStadiumCopy));
        when(stadiumRepository.save(any(Stadium.class))).thenReturn(sharedStadiumCopy);
    }
}