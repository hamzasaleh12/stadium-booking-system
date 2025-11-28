package com.hamza.stadiumbooking.stadium;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stadiums")
public class StadiumController {

    private final StadiumService stadiumService;

    @Autowired
    public StadiumController(StadiumService stadiumService) {
        this.stadiumService = stadiumService;
    }

    @GetMapping
    public ResponseEntity<List<StadiumResponse>> getAllStadiums() {
        return ResponseEntity.ok(stadiumService.getAllStadiums());
    }

    @GetMapping("/{stadiumId}")
    public ResponseEntity<StadiumResponse> getStadiumById(@PathVariable Long stadiumId) {
        return ResponseEntity.ok(stadiumService.getStadiumById(stadiumId));
    }

    @PostMapping
    public ResponseEntity<StadiumResponse> addStadium(@RequestBody StadiumRequest stadiumRequest) {
        StadiumResponse stadiumResponse = stadiumService.addStadium(stadiumRequest);
        return new ResponseEntity<>(stadiumResponse, HttpStatus.CREATED);
    }

    @DeleteMapping("/{stadiumId}")
    public ResponseEntity<Void> deleteStadium(@PathVariable Long stadiumId) {
        stadiumService.deleteStadium(stadiumId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("/{stadiumId}")
    public ResponseEntity<StadiumResponse> updateStadium(
            @PathVariable Long stadiumId,
            @RequestBody StadiumRequest stadiumRequest
    ) {
        StadiumResponse updatedStadium = stadiumService.updateStadium(stadiumId, stadiumRequest);
        return ResponseEntity.ok(updatedStadium);
    }
}