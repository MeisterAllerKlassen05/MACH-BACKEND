package com.example.rideofferservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * Einstiegspunkt der Ride-Offer-Service-Anwendung.
 *
 * <p>Diese Anwendung ermöglicht die Verwaltung von Mitfahrtsangeboten im System.
 * Sie unterstützt das Anbieten, Suchen und Stornieren von Mitfahrten sowie die
 * Kommunikation über RabbitMQ für ereignisgesteuerte Updates.</p>
 *
 * <h2>Hauptverantwortlichkeiten:</h2>
 * <ul>
 *   <li>Anbieten und Verwalten von Mitfahrtsangeboten</li>
 *   <li>Suche nach Mitfahrten basierend auf Start, Ziel und Abfahrtszeit</li>
 *   <li>Stornieren und Löschen von Mitfahrten</li>
 *   <li>Publizieren von Ereignissen (Events) über RabbitMQ</li>
 * </ul>
 *
 * @see com.example.rideofferservice.controller.RideController
 * @see com.example.rideofferservice.service.RideService
 */
@SpringBootApplication
@EnableRabbit
public class RideOfferServiceApplication {

    /**
     * Haupteinstiegspunkt der Anwendung.
     * Startet die Spring Boot-Anwendung und initialisiert den embedded Webserver.
     *
     * @param args Kommandozeilenargumente, die an die SpringApplication weitergegeben werden
     */
	public static void main(String[] args) {
		SpringApplication.run(RideOfferServiceApplication.class, args);
	}
}
