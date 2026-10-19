package com.example.bookingservice.service;

import com.example.bookingservice.entity.Booking;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class RideOfferClient {

    private static final Logger log = LoggerFactory.getLogger(RideOfferClient.class);

    @Value("${app.ride-offer-service.url:http://localhost:8081}")
    private String rideOfferServiceUrl;

    private final RestTemplate restTemplate;

    public RideOfferClient() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        restTemplate = new RestTemplate();
        // Jackson mit Java-Time-Support konfigurieren
        var httpMessageConverter = new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(mapper);
        restTemplate.setMessageConverters(java.util.List.of(httpMessageConverter));
    }

    /**
     * DTO für die Ride-Antwort (muss den Feldern von RideResponse entsprechen).
     */
    public static class RideInfo {
        private Long id;
        private String driverUsername;
        private String startLocation;
        private String endLocation;
        private LocalDateTime departureTime;
        private Integer availableSeats;
        private String additionalInfo;
        private String status;

        public Long getId() { return id; }
        public String getDriverUsername() { return driverUsername; }
        public String getStartLocation() { return startLocation; }
        public String getEndLocation() { return endLocation; }
        public LocalDateTime getDepartureTime() { return departureTime; }
        public Integer getAvailableSeats() { return availableSeats; }
        public String getAdditionalInfo() { return additionalInfo; }
        public String getStatus() { return status; }

        public boolean isAvailable() { return "OFFERED".equals(status); }
    }

    /**
     * Hole eine einzelne Mitfahrt über den Ride-Offer-Service.
     */
    public RideInfo getRideById(Long rideId) {
        String url = rideOfferServiceUrl + "/api/rides/public/" + rideId;
        try {
            ResponseEntity<RideInfo> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, RideInfo.class);
            return response.getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Mitfahrt {} nicht gefunden", rideId);
            return null;
        } catch (Exception e) {
            log.error("Fehler beim Abruf der Mitfahrt {}: {}", rideId, e.getMessage());
            throw new RuntimeException("Kann Ride-Offer-Service nicht erreichen: " + e.getMessage());
        }
    }

    /**
     * Storniere eine Buchung im Ride-Offer-Service (optional).
     */
    public void notifyRideCancelled(Long rideId) {
        String url = rideOfferServiceUrl + "/api/rides/" + rideId + "/cancel";
        try {
            restTemplate.patchForObject(url, null, Map.class);
            log.info("Ride {} storniert über Ride-Offer-Service", rideId);
        } catch (Exception e) {
            log.warn("Fehler beim Stornieren der Mitfahrt {}: {}", rideId, e.getMessage());
        }
    }
}
