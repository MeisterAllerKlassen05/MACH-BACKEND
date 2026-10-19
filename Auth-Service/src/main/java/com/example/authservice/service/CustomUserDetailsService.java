package com.example.authservice.service;

import com.example.authservice.model.entity.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Implementiert Spring Securitys {@link UserDetailsService}.
 *
 * <p>Dient dazu, die Benutzerdaten aus der Datenbank zu laden und sie für die
 * Spring Boot Authentication verfügbar zu machen. Bei erfolgreicher Suche wird
 * ein {@link UserDetails}-Objekt mit den passenden Rollen erstellt.</p>
 *
 * @see UserDetailsService
 * @see com.example.authservice.model.entity.User
 */
//Implementiert Spring Securitys UserDetailsService (Läd Daten und Stellt sie SpringBoots zur Verfügung)gti
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Lädt die Benutzerdaten anhand des angegebenen Benutzernamens.
     *
     * @param username Der anzulegende Benutzername
     * @return Ein {@link UserDetails}-Objekt mit den Informationen des gefundenen Benutzers
     * @throws UsernameNotFoundException Wenn kein Benutzer mit diesem Namen existiert
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isActive()) {
            throw new BadCredentialsException("Account ist deaktiviert");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true, true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
