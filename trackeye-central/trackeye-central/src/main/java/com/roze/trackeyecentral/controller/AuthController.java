// src/main/java/com/trackeye/controller/AuthController.java
package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.UserRepository;
import com.roze.trackeyecentral.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        
        String decryptedPassword = cryptoService.decrypt(user.getPasswordHash());
        if (!decryptedPassword.equals(request.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        
        user.setLastLoginAt(Instant.now().toEpochMilli());
        userRepository.save(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("role", user.getRole());
        response.put("organizationId", user.getOrganizationId());
        response.put("token", generateToken(user));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Validate token and return user info
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
    
    private String generateToken(User user) {
        // Simple token generation - in production use JWT
        return cryptoService.encrypt(user.getId() + ":" + System.currentTimeMillis());
    }
    
    @lombok.Data
    public static class LoginRequest {
        private String email;
        private String password;
    }
}