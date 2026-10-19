package com.example.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Buchungs-Services-Anwendung.
 *
 * <p>Diese Anwendung ermöglicht das Erstellen, Verwalten und Stornieren von
 * Buchungen für Mitfahrten. Sie kommuniziert mit dem Ride-Offer-Service zur
 * Validierung von Mitfahrten und nutzt RabbitMQ für ereignisgesteuerte Updates.</p>
 *
 * <h2>Hauptverantwortlichkeiten:</h2>
 * <ul>
 *   <li>Erstellen und Verwalten von Buchungen</li>
 *   <li>Buchungen nach Mitfahrt oder Passagier abrufen</li>
 *   <li>Stornieren von Buchungen mit Events</li>
 *   <li>Validierung von Mitfahrten über den RideOfferClient</li>
 * </ul>
 *
 * @see com.example.bookingservice.controller.BookingController
 * @see com.example.bookingservice.service.BookingService
 */
@SpringBootApplication
public class BookingServiceApplication {

    /**
     * Haupteinstiegspunkt der Anwendung.
     * Startet die Spring Boot-Anwendung und initialisiert den embedded Webserver.
     *
     * @param args Kommandozeilenargumente, die an die SpringApplication weitergegeben werden
     */
	public static void main(String[] args) {
		SpringApplication.run(BookingServiceApplication.class, args);
	}
}
