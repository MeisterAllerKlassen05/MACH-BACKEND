package com.example.authservice.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Komponente zur Erstellung, Validierung und Auslesung von JWT-Tokens.
 *
 * <p>Verwendet HMAC-SHA zum Signieren der Tokens. Die Geheimnislänge wird bei der Initialisierung geprüft.</p>
 *
 */

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Konstruktor zum Initialisieren des JWT-Token Providers.
     *
     * @param secret       Das geheime Schlüssel für die Token-Signierung (mindestens 32 Zeichen)
     * @param expirationMs Die Gültigkeitsdauer des Tokens in Millisekunden
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret muss mindestens 32 Zeichen lang sein!");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }


    /**
     * Generiert einen Access-Token (JWT, signiert mit HMAC-SHA).
     *
     * @param username Der Benutzername des Subjekts
     * @param role     Die Rolle des Benutzers (wird als Claim gespeichert)
     * @return Das erstellte, signierte JWT-Token-String
     */
    //Generiert einen Access-Token (JWT, signiert mit HMAC-SHA)

    public String generateAccessToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Extrahiert den Benutzernamen (Subject) aus einem Token.
     *
     * @param token Das zu analysierende JWT-Token
     * @return Der im Token gespeicherte Benutzername
     */
    public String getUsernameFromToken(String token) {
        return getClaims(token).getPayload().getSubject();
    }

    /**
     * Extrahiert die Rolle des Benutzers aus einem Token.
     *
     * @param token Das zu analysierende JWT-Token
     * @return Die im Token gespeicherte Rolle
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).getPayload().get("role", String.class);
    }

    /**
     * Validiert ein Token durch Prüfung der Signatur und Ablaufzeit.
     *
     * @param token Das zu validierende JWT-Token
     * @return {@code true} wenn das Token gültig ist, {@code false} sonst
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }
}
