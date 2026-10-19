package com.example.rideofferservice.graphql.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * GraphQL Input-Type für "ridesBySearch"-Query.
 * Mapt automatisch auf das Schema-Input "RideSearchParams".
 */
public record RideSearchParamsInput(
    @NotBlank(message = "Start location is required")
    String startLocation,

    String endLocation,

    String from,

    String to,

    Integer first,

    String after
) {}
