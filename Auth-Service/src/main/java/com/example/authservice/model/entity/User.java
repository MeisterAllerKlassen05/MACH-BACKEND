package com.example.authservice.model.entity;

import jakarta.persistence.*;

/**
 * Entität zur Speicherung und Abbildung eines Benutzers in der Datenbank.
 *
 * <p>Diese Klasse repräsentiert einen registrierten Benutzer im System und enthält
 * alle für die Authentifizierung notwendigen Daten wie Username, Passwort (hashing), Rolle sowie den Aktivierungsstatus.</p>
 *
 * @see com.example.authservice.repository.UserRepository
 */
//Speicherung/Mapping des Users in der DB
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false)
    private boolean active = true;

    /** Standard-Konstruktor für JPA. */
    public User() {}

    /**
     * Erzeugt einen neuen Benutzer mit den angegebenen Daten.
     *
     * @param username  Der eindeutige Benutzername
     * @param password  Das gehashte Passwort des Benutzers
     * @param role      Die Rolle des Benutzers (z.B. "USER")
     * @param active    Gibt an, ob der Account aktiviert ist
     */
    public User(String username, String password, String role, boolean active) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.active = true;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
