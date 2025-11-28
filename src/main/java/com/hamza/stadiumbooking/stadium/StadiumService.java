package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StadiumService {

    private final StadiumRepository stadiumRepository;

    @Autowired
    public StadiumService(StadiumRepository stadiumRepository) {
        this.stadiumRepository = stadiumRepository;
    }

    public List<StadiumResponse> getAllStadiums() {
        return stadiumRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public StadiumResponse getStadiumById(Long id) {
        Stadium stadium = stadiumRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Stadium not found with ID: " + id));
        return mapToDto(stadium);
    }

    public StadiumResponse addStadium(StadiumRequest stadiumRequest) {
        Stadium stadium = mapToEntity(stadiumRequest);
        Stadium savedStadium = stadiumRepository.save(stadium);
        return mapToDto(savedStadium);
    }

    @Transactional
    public StadiumResponse updateStadium(Long id, StadiumRequest request) {
        Stadium stadium = stadiumRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Stadium not found with ID: " + id)
        );

        stadium.setName(request.name() != null ? request.name() : stadium.getName());
        stadium.setLocation(request.location() != null ? request.location() : stadium.getLocation());
        stadium.setPhotoUrl(request.photoUrl() != null ? request.photoUrl() : stadium.getPhotoUrl());
        stadium.setType(request.type() != null ? request.type() : stadium.getType());

        if (request.pricePerHour() != null && request.pricePerHour() > 0) {
            stadium.setPricePerHour(request.pricePerHour());
        }

        if (request.ballRentalFee() != null && request.ballRentalFee() >= 0) {
            stadium.setBallRentalFee(request.ballRentalFee());
        }

        Stadium savedStadium = stadiumRepository.save(stadium);
        return mapToDto(savedStadium);
    }

    public void deleteStadium(Long stadiumId) {
        if (!stadiumRepository.existsById(stadiumId)) {
            throw new ResourceNotFoundException("Stadium not found with ID: " + stadiumId);
        }
        stadiumRepository.deleteById(stadiumId);
    }

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