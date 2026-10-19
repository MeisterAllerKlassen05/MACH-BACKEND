package com.example.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt und Haupteinstieg der Authentifizierungs-API.
 *
 * <p>Diese Anwendung verwaltet die Benutzerregistrierung, Anmeldung (Login),
 * Token-Erzeugung (JWT Access- + Refresh-Token) sowie Logout-Funktionalität
 * für die gesamte MACH Backend-Anwendung.</p>
 *
 * <h2>Hauptverantwortlichkeiten:</h2>
 * <ul>
 *   <li>Benutzerregistrierung mit Passwort-Hashing</li>
 *   <li>Login-Validierung über Spring Security AuthenticationManager</li>
 *   <li>JWT-Zugriffstoken (Access Token) und Refresh-Token ausstellen</li>
 *   <li>Refresh-Token-Verwaltung und Widerrufen beim Logout</li>
 *   <li>Nutzung einer {@code UserRepository} zur Benutzerspeicherung</li>
 * </ul>
 *
 * @see com.example.authservice.controller.AuthController
 * @see com.example.authservice.auth.JwtTokenProvider
 * @see com.example.authservice.service.CustomUserDetailsService
 */
@SpringBootApplication
public class AuthServiceApplication {

    /**
     * Haupteinstiegspunkt der Anwendung.
     * Startet die Spring Boot-Anwendung und initialisiert den embedded Webserver.
     *
     * @param args Kommandozeilenargumente, die an die SpringApplication weitergegeben werden
     */
	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
