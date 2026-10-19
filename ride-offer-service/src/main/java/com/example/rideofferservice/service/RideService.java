package com.example.rideofferservice.service;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.repository.RideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Geschäftslogik-Service für Mitfahrtsangebote.
 *
 * <p>Dient der zentralen Verwaltung von Mitfahrten, einschließlich Erstellen,
 * Suchen, Stornieren und Löschen. Darüber hinaus werden ereignisgesteuerte Updates
 * über RabbitMQ publiziert.</p>
 *
 * <h2>Hauptverantwortlichkeiten:</h2>
 * <ul>
 *   <li>Anbieten und Abrufen von Mitfahrten</li>
 *   <li>Suchen nach Mitfahrten (Start, Ziel, Zeitraum)</li>
 *   <li>Verwalten des Mitfahrtsstatus (OFFERED, CANCELLED, FULFILLED)</li>
 *   <li>Auslösen von RabbitMQ-Events bei Änderungen</li>
 * </ul>
 *
 * @see com.example.rideofferservice.entity.RideOffer
 * @see RideController
 */
@Service
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitmqAvailable;

    @Value("${app.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing.key}")
    private String routingKey;

    /**
     * Konstruktor mit Dependency Injection.
     *
     * <p>Überprüft die Erreichbarkeit von RabbitMQ und initialisiert den
     * {@link RabbitTemplate} entsprechend der Verfügbarkeit.</p>
     *
     * @param rideRepository      Repository zur Persistenz der Mitfahrten
     * @param connectionFactory   ConnectionFactory für RabbitMQ-Verbindungen
     */
    public RideService(RideRepository rideRepository, CachingConnectionFactory connectionFactory) {
        this.rideRepository = rideRepository;
        
        // Prüfe ob RabbitMQ erreichbar ist
        boolean available = false;
        try (Connection conn = connectionFactory.createConnection()) {
            available = true;
        } catch (Exception e) {
            log.warn("RabbitMQ nicht verfügbar: {}", e.getMessage());
        }
        this.rabbitmqAvailable = available;
        
        // Erstelle einen null-safe RabbitTemplate falls RabbitMQ nicht da ist
        RabbitTemplate template;
        try {
            if (available) {
                template = new RabbitTemplate(connectionFactory);
                template.setMessageConverter(new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter());
            } else {
                template = null;
            }
        } catch (Exception e) {
            log.warn("RabbitTemplate konnte nicht erstellt werden, Events werden ignoriert.");
            template = null;
        }
        this.rabbitTemplate = template;
    }

    /**
     * Öffentliche Methode: Alle verfügbaren (angebotenen) Mitfahrten abrufen.
     *
     * @return Liste der {@link RideResponse}-Objekte für alle angebotenen Mitfahrten
     */
    public List<RideResponse> getAvailableRides() {
        return rideRepository.findByStatus(RideOffer.RideStatus.OFFERED)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel.
     *
     * @param startLocation Der Startort der Suchkriterien
     * @param endLocation   Der Zielpunkt der Suchkriterien
     * @return Liste der gefundenen {@link RideResponse}-Objekte
     */
    /**
     * Suche Mitfahrten mit exaktem Start und Ziel.
     */
    public List<RideResponse> searchRides(String startLocation, String endLocation) {
        return rideRepository.findByStartAndEndLocation(startLocation, endLocation)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel innerhalb eines Zeitraums.
     *
     * @param startLocation Der Startort der Suchkriterien
     * @param endLocation   Der Zielpunkt der Suchkriterien
     * @param from          Die untere Zeitgrenze für die Abfahrt
     * @param to            Die obere Zeitgrenze für die Abfahrt
     * @return Liste der gefundenen {@link RideResponse}-Objekte
     */
    /**
     * Suche Mitfahrten mit exaktem Start und Ziel innerhalb eines Zeitraums.
     */
    public List<RideResponse> searchRides(String startLocation, String endLocation, LocalDateTime from, LocalDateTime to) {
        return rideRepository.findByStartAndEndBetween(startLocation, endLocation, from, to)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel ab einem Zeitpunkt.
     *
     * @param startLocation Der Startort der Suchkriterien
     * @param endLocation   Der Zielpunkt der Suchkriterien
     * @param from          Die untere Zeitgrenze für die Abfahrt
     * @return Liste der gefundenen {@link RideResponse}-Objekte
     */
    /**
     * Suche Mitfahrten mit exaktem Start und Ziel ab einem Zeitpunkt.
     */
    public List<RideResponse> searchRidesAfter(String startLocation, String endLocation, LocalDateTime from) {
        return rideRepository.findByStartAndEndAfter(startLocation, endLocation, from)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Suche Mitfahrten mit exaktem Start und Ziel bis zu einem Zeitpunkt.
     *
     * @param startLocation Der Startort der Suchkriterien
     * @param endLocation   Der Zielpunkt der Suchkriterien
     * @param to            Die obere Zeitgrenze für die Abfahrt
     * @return Liste der gefundenen {@link RideResponse}-Objekte
     */
    /**
     * Suche Mitfahrten mit exaktem Start und Ziel bis zu einem Zeitpunkt.
     */
    public List<RideResponse> searchRidesBefore(String startLocation, String endLocation, LocalDateTime to) {
        return rideRepository.findByStartAndEndBefore(startLocation, endLocation, to)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lädt eine einzelne Mitfahrt anhand ihrer ID.
     *
     * @param id Die ID der abzurufenden Mitfahrt
     * @return Das {@link RideResponse}-Objekt oder {@code null}, wenn nicht gefunden
     */
    public RideResponse getRideById(Long id) {
        Optional<RideOffer> ride = rideRepository.findById(id);
        return ride.map(RideResponse::fromEntity).orElse(null);
    }

    /**
     * Lädt alle Mitfahrten eines Fahrers.
     *
     * @param username Der Benutzername des Fahrers
     * @return Liste der {@link RideResponse}-Objekte für die Mitfahrten des Fahrers
     */
    public List<RideResponse> getMyRides(String username) {
        return rideRepository.findByDriverUsername(username)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RideResponse> getMyOfferedRides(String username) {
        return rideRepository.findByStatusAndDriverUsername(RideOffer.RideStatus.OFFERED, username)
                .stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Erstellt ein neues Mitfahrtsangebot.
     *
     * @param username  Der Benutzername des Fahrers
     * @param request   DTO mit den Daten der anzubietenden Mitfahrt
     * @return Das neu erstellte {@link RideResponse}-Objekt
     */
    @Transactional
    public RideResponse createRide(String username, CreateRideRequest request) {
        RideOffer ride = new RideOffer();
        ride.setDriverUsername(username);
        ride.setStartLocation(request.getStartLocation());
        ride.setEndLocation(request.getEndLocation());
        ride.setDepartureTime(request.getDepartureTime());
        ride.setAvailableSeats(request.getAvailableSeats());
        ride.setStatus(RideOffer.RideStatus.OFFERED);

        RideOffer saved = rideRepository.save(ride);

        publishRideOfferedEvent(saved);

        log.info("Neues Mitfahrtsangebot {} erstellt von {}", saved.getId(), username);

        return RideResponse.fromEntity(saved);
    }

    /**
     * Storniert eine Mitfahrt.
     *
     * @param id       Die ID der zu stornierenden Mitfahrt
     * @param username Der Benutzername des Fahrers (zur Berechtigungsprüfung)
     * @return Das aktualisierte {@link RideResponse}-Objekt
     * @throws IllegalStateException Wenn die Mitfahrt bereits storniert oder abgeschlossen ist
     */
    public RideResponse cancelRide(Long id, String username) {
        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (!ride.getDriverUsername().equals(username)) {
            throw new SecurityException("Nur der Fahrer kann die Mitfahrt stornieren!");
        }

        if (ride.getStatus() == RideOffer.RideStatus.FULFILLED) {
            throw new IllegalStateException("Mitfahrt kann nicht mehr storniert werden.");
        }

        ride.setStatus(RideOffer.RideStatus.CANCELLED);
        RideOffer updated = rideRepository.save(ride);

        // Sende RabbitMQ Event für stornierte Mitfahrt
        publishRideCancelledEvent(updated);

        log.info("Mitfahrt {} wurde von {} storniert.", id, username);

        return RideResponse.fromEntity(updated);
    }

    /**
     * Löscht eine Mitfahrt.
     *
     * @param id       Die ID der zu löschenden Mitfahrt
     * @param username Der Benutzername des Fahrers (zur Berechtigungsprüfung)
     * @throws IllegalStateException Wenn die Mitfahrt abgeschlossen ist
     */
    @Transactional
    public void deleteRide(Long id, String username) {
        RideOffer ride = rideRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mitfahrt nicht gefunden!"));

        if (!ride.getDriverUsername().equals(username)) {
            throw new SecurityException("Nur der Fahrer kann die Mitfahrt löschen!");
        }

        if (ride.getStatus() == RideOffer.RideStatus.FULFILLED) {
            throw new IllegalStateException("Eine abgeschlossene Mitfahrt kann nicht gelöscht werden.");
        }

        rideRepository.delete(ride);

        log.info("Mitfahrt {} wurde von {} gelöscht.", id, username);
    }

    /**
     * Publiziert ein {@code RideOfferedEvent} über RabbitMQ.
     *
     * @param ride Die angebotene Mitfahrt, über die informiert werden soll
     */
    // --- RabbitMQ Events ---

    private void publishRideOfferedEvent(RideOffer ride) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate ist null — kann Event nicht senden. Ride gespeichert aber nicht publiziert: {}", ride.getId());
            return;
        }
        try {
            RideOfferedEvent event = new RideOfferedEvent();
            event.setRideId(ride.getId());
            event.setDriverUsername(ride.getDriverUsername());
            event.setStartLocation(ride.getStartLocation());
            event.setEndLocation(ride.getEndLocation());
            event.setDepartureTime(ride.getDepartureTime());
            event.setAvailableSeats(ride.getAvailableSeats());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("RideOfferedEvent gesendet: {} -> {}", ride.getStartLocation(), ride.getEndLocation());
        } catch (Exception e) {
            log.warn("Fehler beim Senden von RideOfferedEvent: {}. Ride wurde gespeichert aber Event ignoriert.", e.getMessage());
        }
    }

    /**
     * Publiziert ein {@code RideCancelledEvent} über RabbitMQ.
     *
     * @param ride Die stornierte Mitfahrt, über die informiert werden soll
     */
    private void publishRideCancelledEvent(RideOffer ride) {
        if (rabbitTemplate == null) return;
        try {
            RideCancelledEvent event = new RideCancelledEvent();
            event.setRideId(ride.getId());
            event.setDriverUsername(ride.getDriverUsername());
            event.setTimestamp(LocalDateTime.now());

            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("RideCancelledEvent gesendet: {}", ride.getId());
        } catch (Exception e) {
            log.warn("Fehler beim Senden von RideCancelledEvent: {}. Ride wurde gespeichert.", e.getMessage());
        }
    }

    /**
     * DTO für das {@code RideOfferedEvent}.
     */
    // --- Event DTOs ---

    public static class RideOfferedEvent {
        private Long rideId;
        private String driverUsername;
        private String startLocation;
        private String endLocation;
        private LocalDateTime departureTime;
        private Integer availableSeats;
        private LocalDateTime timestamp;

        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
        public String getDriverUsername() { return driverUsername; }
        public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
        public String getStartLocation() { return startLocation; }
        public void setStartLocation(String startLocation) { this.startLocation = startLocation; }
        public String getEndLocation() { return endLocation; }
        public void setEndLocation(String endLocation) { this.endLocation = endLocation; }
        public LocalDateTime getDepartureTime() { return departureTime; }
        public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
        public Integer getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(Integer availableSeats) { this.availableSeats = availableSeats; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    /**
     * DTO für das {@code RideCancelledEvent}.
     */
    public static class RideCancelledEvent {
        private Long rideId;
        private String driverUsername;
        private LocalDateTime timestamp;

        public Long getRideId() { return rideId; }
        public void setRideId(Long rideId) { this.rideId = rideId; }
        public String getDriverUsername() { return driverUsername; }
        public void setDriverUsername(String driverUsername) { this.driverUsername = driverUsername; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
