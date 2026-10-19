package com.example.rideofferservice.graphql;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.graphql.dto.CreateRideInput;
import com.example.rideofferservice.service.RideService;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * GraphQL Mutation-Resolver für Ride-Mutations (create, cancel, delete).
 */
@Component
public class RideMutationResolver {

    private final RideService rideService;

    @Autowired
    public RideMutationResolver(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Neue Mitfahrt anbieten.
     *
     * @param input GraphQL Input-Type "CreateRideInput" (mapt auf Schema-Input).
     */
    @PreAuthorize("isAuthenticated()")
    public RideResponse createRide(CreateRideInput input) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";

        CreateRideRequest request = new CreateRideRequest(
                input.startLocation(),
                input.endLocation(),
                LocalDateTime.parse(input.departureTime()),
                input.availableSeats(),
                input.additionalInfo()
        );

        return rideService.createRide(username, request);
    }

    /**
     * Mitfahrt stornieren.
     */
    @PreAuthorize("isAuthenticated()")
    public RideResponse cancelRide(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        return rideService.cancelRide(id, username);
    }

    /**
     * Mitfahrt löschen.
     */
    @PreAuthorize("isAuthenticated()")
    public boolean deleteRide(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        rideService.deleteRide(id, username);
        return true;
    }
}
