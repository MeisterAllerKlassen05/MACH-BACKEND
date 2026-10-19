package com.example.rideofferservice.controller;

import com.example.rideofferservice.dto.CreateRideRequest;
import com.example.rideofferservice.dto.RideResponse;
import com.example.rideofferservice.entity.RideOffer;
import com.example.rideofferservice.service.RideService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-Controller für alle Mitfahrtsangebote.
 *
 * <p>Dient der Bereitstellung von Endpunkten zur Erstellung, Suche, Aktualisierung
 * und Löschung von Mitfahrten. Einige Endpunkte sind öffentlich (für die Suche),
 * während andere eine Authentifizierung erfordern.</p>
 *
 * <h2>Verwendete Endpunkte:</h2>
 * <ul>
 *   <li>{@code GET /api/rides/public/available} – Öffentlicher Endpunkt für alle verfügbaren Mitfahrten</li>
 *   <li>{@code GET /api/rides/public/search} – Öffentlicher Such-Endpunkt für Mitfahrten</li>
 *   <li>{@code GET /api/rides/my} – Geschützter Endpunkt für die eigenen Mitfahrten des Benutzers</li>
 *   <li>{@code GET /api/rides/my/offered} – Geschützter Endpunkt für angebotene eigene Mitfahrten</li>
 *   <li>{@code POST /api/rides} – Geschützter Endpunkt zum Anbieten einer neuen Mitfahrt</li>
 *   <li>{@code PATCH /api/rides/{id}/cancel} – Geschützter Endpunkt zum Stornieren einer Mitfahrt</li>
 *   <li>{@code DELETE /api/rides/{id}} – Geschützter Endpunkt zum Löschen einer Mitfahrt</li>
 *   <li>{@code GET /api/rides/{id}} – Öffentlicher Endpunkt für eine einzelne Mitfahrt nach ID</li>
 * </ul>
 *
 * @see com.example.rideofferservice.service.RideService
 * @see com.example.rideofferservice.entity.RideOffer
 */
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    /**
     * Konstruktor mit Dependency Injection.
     *
     * @param rideService Der Service zur Geschäftslogik für Mitfahrten
     */
    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Öffentlicher Endpunkt: Alle verfügbaren (angebotenen) Mitfahrten abrufen.
     *
     * @return ResponseEntity mit einer Liste aller verfügbaren Mitfahrten
     */
    @GetMapping("/public/available")
    public ResponseEntity<List<RideResponse>> getAvailableRides() {
        return ResponseEntity.ok(rideService.getAvailableRides());
    }

    /**
     * Öffentlicher Endpunkt: Mitfahrten suchen nach Start, Ziel und Abfahrtszeit.
     *
     * @param startLocation Der Startort der Suche
     * @param endLocation   Der Zielpunkt der Suche
     * @param from          Optionale untere Zeitgrenze für die Abfahrt
     * @param to            Optionale obere Zeitgrenze für die Abfahrt
     * @return ResponseEntity mit einer Liste der gefundenen Mitfahrten
     */
    @GetMapping("/public/search")
    public ResponseEntity<List<RideResponse>> searchRides(
            @RequestParam String startLocation,
            @RequestParam String endLocation,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to) {
        List<RideResponse> rides;

        if (from != null && to != null) {
            rides = rideService.searchRides(startLocation, endLocation, from, to);
        } else if (from != null) {
            rides = rideService.searchRidesAfter(startLocation, endLocation, from);
        } else if (to != null) {
            rides = rideService.searchRidesBefore(startLocation, endLocation, to);
        } else {
            rides = rideService.searchRides(startLocation, endLocation);
        }

        return ResponseEntity.ok(rides);
    }

    /**
     * Authentifizierter Endpunkt: Eigene Mitfahrten abrufen.
     *
     * @param principal Das aktuell authentifizierte Prinzipal-Objekt
     * @return ResponseEntity mit einer Liste der Mitfahrten des Benutzers
     */
    @GetMapping("/my")
    public ResponseEntity<List<RideResponse>> getMyRides(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(rideService.getMyRides(username));
    }

    /**
     * Authentifizierter Endpunkt: Eigene angebotene Mitfahrten abrufen.
     *
     * @param principal Das aktuell authentifizierte Prinzipal-Objekt
     * @return ResponseEntity mit einer Liste der vom Benutzer angebotenen Mitfahrten
     */
    @GetMapping("/my/offered")
    public ResponseEntity<List<RideResponse>> getMyOfferedRides(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(rideService.getMyOfferedRides(username));
    }

    /**
     * Authentifizierter Endpunkt: Neue Mitfahrt anbieten (jeder registrierte User).
     *
     * @param request   DTO mit den Daten der anzubietenden Mitfahrt
     * @param principal Das aktuell authentifizierte Prinzipal-Objekt
     * @return ResponseEntity mit dem neu erstellten Fahrtdaten als {@link RideResponse} (Statuscode 201)
     */
    @PostMapping
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            Principal principal) {
        String username = principal.getName();

        RideResponse created = rideService.createRide(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Authentifizierter Endpunkt: Mitfahrt abbrechen/stornieren.
     *
     * @param id        Die ID der zu stornierenden Mitfahrt
     * @param principal Das aktuell authentifizierte Prinzipal-Objekt
     * @return ResponseEntity mit den aktualisierten Fahrtdaten als {@link RideResponse}
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<RideResponse> cancelRide(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        RideResponse cancelled = rideService.cancelRide(id, username);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * Authentifizierter Endpunkt: Mitfahrt löschen (nur Fahrer).
     *
     * @param id        Die ID der zu löschenden Mitfahrt
     * @param principal Das aktuell authentifizierte Prinzipal-Objekt
     * @return ResponseEntity mit Statuscode 204 (NO_CONTENT) bei Erfolg
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(
            @PathVariable Long id,
            Principal principal) {
        String username = principal.getName();
        rideService.deleteRide(id, username);
        return ResponseEntity.noContent().build();
    }

    /**
     * Öffentlicher Endpunkt: Einzelne Mitfahrt nach ID abrufen.
     *
     * @param id Die ID der abzurufenden Mitfahrt
     * @return ResponseEntity mit den Fahrtdaten als {@link RideResponse} oder 404 (NOT_FOUND)
     */
    @GetMapping("/{id}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long id) {
        RideResponse ride = rideService.getRideById(id);
        if (ride == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ride);
    }
}
