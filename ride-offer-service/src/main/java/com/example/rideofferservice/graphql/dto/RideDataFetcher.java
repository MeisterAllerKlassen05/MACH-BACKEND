package com.example.rideofferservice.graphql.dto;

import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.repository.RideRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class RideDataFetcher {

    private final RideRepository rideRepository;

    @Autowired
    public RideDataFetcher(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public List<RideResponse> availableRides() {
        return rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)
                .stream()
                .map(RideResponse::fromEntity)
                .toList();
    }

    public List<RideResponse> ridesByLocation(String startLocation, String endLocation) {
        if (endLocation != null && !endLocation.isBlank()) {
            return rideRepository.findByStartAndEndLocation(startLocation, endLocation)
                    .stream()
                    .map(RideResponse::fromEntity)
                    .toList();
        }
        // Fallback: Suche nur nach Start-Location
        List<RideOffer> rides = rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)
                .stream()
                .filter(r -> r.getStartLocation().equals(startLocation))
                .toList();
        return rides.stream().map(RideResponse::fromEntity).toList();
    }

    public Optional<RideResponse> ride(Long id) {
        return rideRepository.findById(id)
                .map(RideResponse::fromEntity);
    }

    public List<RideResponse> myOfferedRides() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        if (username == null) return List.of();

        return rideRepository.findByStatusAndDriverUsername(RideOffer.RideStatus.OFFERED, username)
                .stream()
                .map(RideResponse::fromEntity)
                .toList();
    }
}
