package com.example.bookingservice.service;

import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CreateBookingRequest;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Geschäftslogik-Service für Buchungen.
 *
 * <p>Dient der zentralen Verwaltung von Buchungen, einschließlich Erstellung, Suche
 * und Stornierung. Darüber hinaus werden ereignisgesteuerte Updates über RabbitMQ
 * publiziert.</p>
 *
 * <h2>Hauptverantwortlichkeiten:</h2>
 * <ul>
 * <li>Erstellen von Buchungen für Mitfahrten</li>
 * <li>Abrufen von Buchungen nach Mitfahrt oder Passagier</li>
 * <li>Stornieren von Buchungen mit Event-Auslösung</li>
 * <li>Validierung der Mitfahrt-Verfügbarkeit über den RideOfferClient</li>
 * </ul>
 *
 * @see com.example.bookingservice.entity.Booking
 * @see BookingController
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final RideOfferClient rideOfferClient;
    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitmqAvailable;

    @Value("${app.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing.key.booking:booking.routing}")
    private String bookingRoutingKey;

    /**
     * Konstruktor mit Dependency Injection.
     *
     * <p>Überprüft die Erreichbarkeit von RabbitMQ und initialisiert den
     * {@link RabbitTemplate} entsprechend der Verfügbarkeit.</p>
     *
     * @param bookingRepository      Repository zur Persistenz der Buchungen
     * @param rideOfferClient        Client zum Abrufen von Mitfahrtinformationen
     * @param connectionFactory      ConnectionFactory für RabbitMQ-Verbindungen
     */
    public BookingService(BookingRepository bookingRepository,
                          RideOfferClient rideOfferClient,
                          org.springframework.amqp.rabbit.connection.CachingConnectionFactory connectionFactory) {
        this.bookingRepository = bookingRepository;
        this.rideOfferClient = rideOfferClient;

        boolean available = false;
        try (var conn = connectionFactory.createConnection()) {
            available = true;
        } catch (Exception e) {
            log.warn("RabbitMQ nicht verfügbar: {}", e.getMessage());
        }

        this.rabbitmqAvailable = available;

        org.springframework.amqp.rabbit.core.RabbitTemplate template;
        try {
            if (available) {
                template = new RabbitTemplate(connectionFactory);
                template.setMessageConverter(new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter());
            } else {
                template = null;
            }
        } catch (Exception e) {
            log.warn("RabbitTemplate konnte nicht erstellt werden.");
            template = null;
        }
        this.rabbitTemplate = template;
    }

    /**
     * Erstellt eine neue Buchung für eine Mitfahrt.
     *
     * <p>Validiert zuerst, ob die angegebene Mitfahrt existiert und noch verfügbar ist,
     * und prüft zudem, ob bereits eine aktive Buchung vorliegt und genug freie Plätze vorhanden sind.</p>
     *
     * @param username  Der Benutzername des Passagiers
     * @param request   DTO mit der ID der anzubuchenden Mitfahrt
     * @return Das neu erstellte {@link BookingResponse}-Objekt
     * @throws IllegalArgumentException Wenn die Ride-ID fehlt oder die Mitfahrt nicht gefunden wurde
     * @throws IllegalStateException    Wenn die Mitfahrt nicht mehr verfügbar ist oder keine Plätze frei sind
     */
    /**
     * Neue Buchung erstellen.
     */
    @Transactional
    public BookingResponse createBooking(String username, CreateBookingRequest request) {
        // Validiere: Ride ID muss übergeben werden
        if (request.getRideId() == null) {
            throw new IllegalArgumentException("Ride ID is required");
        }

        Long rideId = request.getRideId();

        // Prüfe ob die Mitfahrt existiert und noch verfügbar ist
        RideOfferClient.RideInfo rideInfo = rideOfferClient.getRideById(rideId);
        if (rideInfo == null) {
            throw new IllegalArgumentException("Mitfahrt nicht gefunden: " + rideId);
        }

        // Prüfe ob die Mitfahrt noch "OFFERED" ist
        String status = rideInfo.getStatus();
        if (!"OFFERED".equals(status)) {
            throw new IllegalStateException("Die Mitfahrt ist nicht mehr verfügbar. Status: " + status);
        }

        // Prüfe ob die Mitfahrt in der Vergangenheit liegt
        java.time.LocalDateTime departureTime = rideInfo.getDepartureTime();
        if (departureTime != null && departureTime.isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalStateException("Die Abfahrtszeit liegt bereits in der Vergangenheit.");
        }

        // Prüfe ob die Buchung schon existiert
        if (bookingRepository.existsByRideIdAndStatus(rideId, Booking.BookingStatus.CONFIRMED)) {
            throw new IllegalStateException("Es gibt bereits eine bestätigte Buchung für diese Mitfahrt.");
        }

        // Prüfe ob genug Plätze frei sind
        Integer availableSeats = rideInfo.getAvailableSeats();
        if (availableSeats != null && availableSeats <= 0) {
            throw new IllegalStateException("Keine freien Plätze mehr verfügbar für diese Mitfahrt.");
        }

        // Buchung erstellen
        Booking booking = new Booking(rideId, username);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Neue Buchung {} erstellt für User {}, Ride {}", 
                savedBooking.getId(), username, rideId);

        // Sende RabbitMQ Event dass eine Buchung erstellt wurde
        if (rabbitTemplate != null) {
            try {
                Map<String, Object> event = Map.of(
                    "bookingId", savedBooking.getId(),
                    "rideId", rideId,
                    "passengerUsername", username,
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                rabbitTemplate.convertAndSend(exchangeName, bookingRoutingKey + ".created", event);
            } catch (Exception e) {
                log.warn("Fehler beim Senden von BookingCreatedEvent: {}", e.getMessage());
            }
        }

        return BookingResponse.fromEntity(savedBooking);
    }

    /**
     * Alle Buchungen für eine Mitfahrt abrufen.
     *
     * @param rideId Die ID der Mitfahrt, für die die Buchungen abgerufen werden sollen
     * @return Liste der {@link BookingResponse}-Objekte für alle Buchungen der Mitfahrt
     */
    /**
     * Alle Buchungen für eine Mitfahrt abrufen.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForRide(Long rideId) {
        return bookingRepository.findByRideId(rideId)
                .stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Alle Buchungen eines Passagiers abrufen.
     *
     * <p>Nur bestätigte (nicht stornierte) Buchungen werden zurückgegeben.</p>
     *
     * @param username Der Benutzername des Passagiers
     * @return Liste der {@link BookingResponse}-Objekte für alle BESTÄTIGTEN Buchungen des Passagiers
     */
    /**
     * Alle BESTÄTIGTEN Buchungen eines Passagiers abrufen.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByPassenger(String username) {
        return bookingRepository.findByPassengerUsernameAndStatus(username, Booking.BookingStatus.CONFIRMED)
                .stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Buchung stornieren.
     *
     * @param bookingId Die ID der zu stornierenden Buchung
     * @param username  Der Benutzername des Passagiers (zur Berechtigungsprüfung)
     * @throws IllegalStateException Wenn die Buchung bereits storniert ist
     */
    /**
     * Buchung stornieren.
     */
    @Transactional
    public void cancelBooking(Long bookingId, String username) {
        Booking booking = bookingRepository.findByIdAndPassengerUsername(bookingId, username)
                .orElseThrow(() -> new RuntimeException("Buchung nicht gefunden oder gehört nicht zu diesem Benutzer."));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Buchung wurde bereits storniert.");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Sende Event zum Stornieren der Buchung über RabbitMQ
        if (rabbitTemplate != null) {
            try {
                Map<String, Object> event = Map.of(
                    "bookingId", booking.getId(),
                    "rideId", booking.getRideId(),
                    "passengerUsername", booking.getPassengerUsername(),
                    "timestamp", java.time.LocalDateTime.now().toString()
                );
                rabbitTemplate.convertAndSend(exchangeName, bookingRoutingKey + ".cancelled", event);
            } catch (Exception e) {
                log.warn("Fehler beim Senden von BookingCancelledEvent: {}", e.getMessage());
            }
        }

        log.info("Buchung {} storniert von User {}", bookingId, username);
    }
}
