package com.roze.trackeyecentral.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Proper one-way password hashing with BCrypt.
 *
 * This replaces storing passwords with reversible AES encryption (the old code
 * did cryptoService.encrypt(password) and then decrypt-and-compare on login,
 * which means anyone who reads the database can recover every password).
 *
 * BCrypt is one-way and salted: you can only verify a password, never recover it.
 *
 * Requires the spring-security-crypto dependency (added to pom.xml).
 */
@Service
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null) return false;
        return encoder.matches(rawPassword, storedHash);
    }
}
