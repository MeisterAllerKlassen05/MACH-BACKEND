package com.example.rideofferservice.graphql.dto;

/**
 * GraphQL-typ für RideResponse.
 * Wird als Java-Vertreter für den GraphQL-Typ "RideResponse" verwendet.
 */
public class GraphQLRideResponse {

    private Long id;
    private String driverUsername;
    private String startLocation;
    private String endLocation;
    private String departureTime;
    private Integer availableSeats;
    private String additionalInfo;
    private String status;
    private String createdAt;

    public GraphQLRideResponse() {}

    // --- Getters & Setters (Standard Java-Bean) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriverUsername() { return driverUsername; }
    public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }

    public String getStartLocation() { return startLocation; }
    public void setStartLocation(String startLocation) { this.startLocation = startLocation; }

    public String getEndLocation() { return endLocation; }
    public void setEndLocation(String endLocation) { this.endLocation = endLocation; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public Integer getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
