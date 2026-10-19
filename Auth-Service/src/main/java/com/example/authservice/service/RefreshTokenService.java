package com.example.authservice.service;

import com.example.authservice.model.entity.RefreshToken;
import com.example.authservice.model.entity.User;
import com.example.authservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service zur Verwaltung von Refresh-Tokens.
 *
 * <p>Dient der Erstellung, Validierung und dem Widerrufen von Refresh-Tokens
 * im Rahmen des JWT-basierten Authentifizierungsprozesses. Jeder User kann jeweils
 * nur ein gültiges Refresh-Token besitzen.</p>
 *
 * @see com.example.authservice.auth.JwtTokenProvider
 * @see com.example.authservice.model.entity.RefreshToken
 */
@Service
public class RefreshTokenService {

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshExpiryMs;

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Konstruktor mit Dependency Injection.
     *
     * @param refreshTokenRepository Repository zur Persistenz der Refresh-Tokens
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Erstellt einen neuen Refresh-Token für den angegebenen Benutzer.
     *
     * <p>Zuvor werden alle bestehenden Refresh-Tokens des Benutzers widerrufen.</p>
     *
     * @param user Der Benutzer, für den das Token erstellt werden soll
     * @return Das neu erstellte und gespeicherte {@link RefreshToken}-Objekt
     */
    //Erstellt einen neuen Refresh-Token für den given User.
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        revokeRefreshTokensByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshExpiryMs);

        RefreshToken refreshToken = new RefreshToken(token, expiryDate, user);
        return refreshTokenRepository.save(refreshToken);
    }


    /**
     * Validiert und gibt den Refresh-Token zurück.
     *
     * <p>Wirft eine {@link RuntimeException}, wenn das Token ungültig, abgelaufen oder nicht gefunden ist.</p>
     *
     * @param token Der zu validierende Refresh-Token-String
     * @return Das gültige {@link RefreshToken}-Objekt
     * @throws RuntimeException Wenn das Token nicht existiert oder abgelaufen ist
     */
     //Validiert und gibt den Refresh-Token zurück. Wirft Exception wenn ungültig/abgelaufen/nicht gefunden.
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Ungültiger Refresh-Token"));

        if (refreshToken.isExpired()) {
            deleteRefreshToken(refreshToken);
            throw new RuntimeException("Refresh-Token ist abgelaufen");
        }

        return refreshToken;
    }

    /**
     * Widerruft (löscht) den angegebenen Refresh-Token.
     *
     * @param token Der zu widerrufende Token-String
     */
     //Widerruft (löscht) den Refresh-Token.
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(this::deleteRefreshToken);
    }

    /**
     * Löscht das angegebene Refresh-Token aus der Datenbank.
     *
     * @param refreshToken Das zu löschende {@link RefreshToken}-Objekt
     */
    //Löscht Refreshtoken aus der DB.
    @Transactional
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    /**
     * Widerruft alle Refresh-Tokens eines Benutzers (wichtig für Logout).
     *
     * @param user Der Benutzer, dessen Tokens widerrufen werden sollen
     */
    //Widerruft alle Refresh-Tokens eines Users (wichtig für Logout).
    private void revokeRefreshTokensByUser(User user) {
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()))
                .forEach(this::deleteRefreshToken);
    }
}
