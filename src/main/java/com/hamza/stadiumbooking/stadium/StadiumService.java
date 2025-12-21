package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class StadiumService {

    private final StadiumRepository stadiumRepository;
    private final UserRepository userRepository;
    private final OwnershipValidationService ownershipValidationService;

    public Page<StadiumResponse> getAllStadiums(Pageable pageable) {
        log.info("Action: getAllStadiums | Fetching stadiums from database");
        return stadiumRepository.findAllByIsDeletedFalse(pageable).map(this::mapToDto);
    }

    @Cacheable(value = "locations")
    public List<String> getAllLocations() {
        log.info("Action: getAllLocations | Fetching distinct locations from database (Cache Miss)");
        return stadiumRepository.findAllDistinctLocations();
    }

    public StadiumResponse getStadiumById(Long id) {
        Stadium stadium = stadiumRepository.findByIdAndIsDeletedFalse(id).orElseThrow(
                () -> {
                    log.error("Action: getStadiumById | Error: Stadium not found with ID: {}", id);
                    return new ResourceNotFoundException("Stadium not found with ID: " + id);
                });
        log.info("Action: getStadiumById | Successfully retrieved stadium: {}", id);
        return mapToDto(stadium);
    }

    // TODO: Refactor BookingService to use this method instead of Repository for better layer separation
    public Stadium findStadiumEntityById(Long id) {
        log.debug("Action: findStadiumEntityById | Validating stadium for internal booking use: {}", id);
        return stadiumRepository.findByIdAndIsDeletedFalse(id).orElseThrow(
                () -> new ResourceNotFoundException("Stadium linked to booking not found: " + id));
    }

    @CacheEvict(value = {"locations"}, allEntries = true)
    public StadiumResponse addStadium(StadiumRequest request) {
        log.info("Action: addStadium | Attempting to add new stadium: {}", request.name());
        Stadium stadium = mapToEntity(request);

        Long managerId = ownershipValidationService.getCurrentUserId();
        if (managerId == null) {
            log.error("Action: addStadium | Error: Current user ID is null");
            throw new IllegalStateException("Error: Unable to determine the current user. User not authenticated.");
        }
        User manger = userRepository.findByIdAndIsDeletedFalse(managerId).orElseThrow(
                () -> {
                    log.error("Action: addStadium | Error: Manager ID {} not found", managerId);
                    return new ResourceNotFoundException("Manager not found with ID: " + managerId);
                }
        );

        stadium.setOwner(manger);
        Stadium savedStadium = stadiumRepository.save(stadium);
        log.info("Action: addStadium | Success | Stadium created with ID: {} and linked to Manager: {}", savedStadium.getId(), managerId);
        log.debug("Action: addStadium | Cache evicted for 'stadiums' and 'locations'");
        return mapToDto(savedStadium);
    }

    @Transactional
    @CacheEvict(value = {"locations"}, allEntries = true)
    public void deleteStadium(Long stadiumId) {
        log.info("Action: deleteStadium | Attempting to soft-delete stadium ID: {}", stadiumId);
        Stadium stadium = stadiumRepository.findByIdAndIsDeletedFalse(stadiumId).orElseThrow(
                () -> {
                    log.error("Action: deleteStadium | Error: Stadium {} not found for deletion", stadiumId);
                    return new ResourceNotFoundException("Stadium not found with ID: " + stadiumId);
                });

        stadium.setDeleted(true);
        Stadium savedStadium = stadiumRepository.save(stadium);
        log.info("Action: deleteStadium | Success | Stadium ID: {} marked as deleted", savedStadium.getId());
    }

    @Transactional
    @CacheEvict(value = {"locations"}, allEntries = true)
    public StadiumResponse updateStadium(Long id, StadiumRequestForUpdate request) {
        log.info("Action: updateStadium | Attempting to update stadium ID: {}", id);
        Stadium stadium = stadiumRepository.findByIdAndIsDeletedFalse(id).orElseThrow(
                () -> {
                    log.error("Action: updateStadium | Error: Stadium {} not found", id);
                    return new ResourceNotFoundException("Stadium not found with ID: " + id);
                }
        );

        stadium.setName((request.name() != null && !request.name().isEmpty()) ? request.name() : stadium.getName());
        stadium.setPhotoUrl(request.photoUrl() != null ? request.photoUrl() : stadium.getPhotoUrl());

        if (request.pricePerHour() != null && request.pricePerHour() > 0) {
            stadium.setPricePerHour(request.pricePerHour());
        }

        if (request.ballRentalFee() != null && request.ballRentalFee() >= 0) {
            stadium.setBallRentalFee(request.ballRentalFee());
        }

        Stadium savedStadium = stadiumRepository.save(stadium);
        log.info("Action: updateStadium | Success | Stadium ID: {} updated successfully", savedStadium.getId());
        return mapToDto(savedStadium);
    }
    // ... Helper methods ....
    private StadiumResponse mapToDto(Stadium stadium) {
        return new StadiumResponse(
                stadium.getId(),
                stadium.getName(),
                stadium.getLocation(),
                stadium.getPricePerHour(),
                stadium.getBallRentalFee(),
                stadium.getType(),
                stadium.getPhotoUrl()
        );
    }

    private Stadium mapToEntity(StadiumRequest request) {
        Stadium stadium = new Stadium();
        stadium.setName(request.name());
        stadium.setLocation(request.location());
        stadium.setPhotoUrl(request.photoUrl());
        stadium.setPricePerHour(request.pricePerHour());
        stadium.setBallRentalFee(request.ballRentalFee() != null ? request.ballRentalFee() : 0);
        stadium.setType(request.type());
        return stadium;
    }
}