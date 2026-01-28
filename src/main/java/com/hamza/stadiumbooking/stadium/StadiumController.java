package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.security.utils.OwnershipValidationService;
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
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;


@RestController @RequiredArgsConstructor @Slf4j
@RequestMapping("/api/v1/stadiums")
public class StadiumController {

    private final StadiumService stadiumService;
    private final OwnershipValidationService ownershipValidationService;

    @GetMapping
    public ResponseEntity<Page<StadiumResponse>> getAllStadiums(@ParameterObject
                                                                @PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable){
        log.info("Incoming request to get all stadiums | Page: {}", pageable.getPageNumber());
        return ResponseEntity.ok(stadiumService.getAllStadiums(pageable));
    }

    @GetMapping("/{stadiumId}")
    public ResponseEntity<StadiumResponse> getStadiumById(@PathVariable UUID stadiumId) {
        log.info("Incoming request to get stadium with ID: {}", stadiumId);
        return ResponseEntity.ok(stadiumService.getStadiumById(stadiumId));
    }

    // TODO: Implement Search Feature (Phase 2) - Priority: Medium
    // @GetMapping("/search")
    // public ResponseEntity<PageResponse<StadiumResponse>> search(...) { ... }

    @GetMapping("/locations")
    public ResponseEntity<List<String>> getAllLocations() {
        log.info("Incoming request to get all stadium locations");
        return ResponseEntity.ok(stadiumService.getAllLocations());
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<StadiumResponse> addStadium(@RequestBody @Valid StadiumRequest request){
        log.info("Incoming request to create stadium '{}' by Manager ID: {}",
                request.name(), ownershipValidationService.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(stadiumService.addStadium(request));
    }

    @DeleteMapping("/{stadiumId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @ownershipValidationService.isStadiumOwner(#stadiumId))")
    public ResponseEntity<Void> deleteStadium(@PathVariable UUID stadiumId) {
        log.info("Incoming request to delete stadium ID: {} by User ID: {}",
                stadiumId, ownershipValidationService.getCurrentUserId());
        stadiumService.deleteStadium(stadiumId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{stadiumId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @ownershipValidationService.isStadiumOwner(#stadiumId))")
    public ResponseEntity<StadiumResponse> updateStadium(
            @PathVariable UUID stadiumId,
            @RequestBody @Valid StadiumRequestForUpdate stadiumRequestForUpdate
    ) {
        log.info("Incoming request to update stadium ID: {} by User ID: {}",
                stadiumId, ownershipValidationService.getCurrentUserId());
        StadiumResponse updatedStadium = stadiumService.updateStadium(stadiumId, stadiumRequestForUpdate);
        return ResponseEntity.ok(updatedStadium);
    }
}