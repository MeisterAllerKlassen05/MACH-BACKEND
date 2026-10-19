package com.example.rideofferservice.graphql.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * GraphQL Input-Type für "createRide"-Mutation.
 * Mapt automatisch auf das Schema-Input "CreateRideInput".
 */
public record CreateRideInput(

    @NotBlank(message = "Start location is required")
    String startLocation,

    @NotBlank(message = "End location is required")
    String endLocation,

    @NotNull(message = "Departure time is required")
    String departureTime,

    @NotNull(message = "Available seats is required")
    Integer availableSeats,

    String additionalInfo
) {}
