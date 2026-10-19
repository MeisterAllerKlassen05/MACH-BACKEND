package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.dto.CreateBookingRequest;
import com.example.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-Controller für alle Buchungs-Endpunkte.
 *
 * <p>Dient der Bereitstellung von Endpunkten zur Erstellung, Suche und Löschung von
 * Buchungen. Alle Endpunkte erfordern eine Authentifizierung.</p>
 *
 * <h2>Verwendete Endpunkte:</h2>
 * <ul>
 *   <li>{@code POST /api/bookings} – Erstellen einer neuen Buchung</li>
 *   <li>{@code GET /api/bookings/ride/{rideId}} – Abrufen aller Buchungen für eine Mitfahrt</li>
 *   <li>{@code GET /api/bookings/my-bookings} – Abrufen der eigenen Buchungen</li>
 *   <li>{@code DELETE /api/bookings/{bookingId}} – Stornieren einer Buchung</li>
 * </ul>
 *
 * @see com.example.bookingservice.service.BookingService
 * @see com.example.bookingservice.entity.Booking
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Konstruktor mit Dependency Injection.
     *
     * @param bookingService Der Service zur Geschäftslogik für Buchungen
     */
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Erstellt eine neue Buchung für eine Mitfahrt.
     *
     * <p>Validiert die Mitfahrt über den RideOfferClient und speichert die Buchung.</p>
     *
     * @param request          DTO mit den Daten der zu erstellenden Buchung
     * @param authentication   Das aktuell authentifizierte Benutzer-Objekt
     * @return ResponseEntity mit dem neu erstellten Buchungsdaten bei Erfolg,
     *         400 (BAD_REQUEST) bei Validierungsfehlern oder 500 (INTERNAL_SERVER_ERROR) bei Fehlern
     */
    @PostMapping
    public ResponseEntity<?> createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        try {
            BookingResponse booking = bookingService.createBooking(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Interner Fehler: " + e.getMessage()));
        }
    }

    /**
     * Ruft alle Buchungen für eine bestimmte Mitfahrt ab.
     *
     * @param rideId Die ID der Mitfahrt, für die die Buchungen abgerufen werden sollen
     * @return ResponseEntity mit einer Liste der gefundenen {@link BookingResponse}-Objekte
     */
    @GetMapping("/ride/{rideId}")
    public ResponseEntity<List<BookingResponse>> getBookingsForRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(bookingService.getBookingsForRide(rideId));
    }

    /**
     * Ruft alle Buchungen des aktuell authentifizierten Benutzers ab.
     *
     * @param authentication Das aktuell authentifizierte Benutzer-Objekt
     * @return ResponseEntity mit einer Liste der gefundenen {@link BookingResponse}-Objekte
     */
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingsByPassenger(authentication.getName()));
    }

    /**
     * Storniert eine Buchung.
     *
     * @param bookingId      Die ID der zu stornierenden Buchung
     * @param authentication Das aktuell authentifizierte Benutzer-Objekt
     * @return ResponseEntity mit Statuscode 200 (OK) und Erfolgsmeldung bei Erfolg,
     *         403 (FORBIDDEN) bei fehlender Berechtigung oder 404 (NOT_FOUND) wenn die Buchung nicht existiert
     */
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(@PathVariable Long bookingId, Authentication authentication) {
        try {
            bookingService.cancelBooking(bookingId, authentication.getName());
            return ResponseEntity.ok(Map.of("message", "Buchung erfolgreich gelöscht!"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
