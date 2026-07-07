package com.roze.api;

import com.roze.service.InstallationService;
import com.roze.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lets the employee connect this machine to the central server.
 *
 * The desktop app already runs a small local web server on port 8765, so we
 * expose the setup here. The employee opens http://localhost:8765/setup (or
 * you can POST from the tray UI) and provides:
 *   - email                (their account email in the organization)
 *   - registrationToken    (the token the admin/invite gave them)
 *
 * On success the API key + device id are written to ~/.trackeye/config.properties
 * and the SyncService starts uploading immediately.
 */
@Slf4j
@RestController
@RequestMapping("/api/setup")
@RequiredArgsConstructor
public class SetupController {

    private final InstallationService installationService;
    private final SyncService syncService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("registered", syncService.isRegistered()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest body) {
        if (body.getEmail() == null || body.getRegistrationToken() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "email and registrationToken are required"));
        }

        InstallationService.RegistrationResult result =
                installationService.registerDevice(body.getEmail(), body.getRegistrationToken());

        if (result.isSuccess()) {
            // Pick up the freshly written API key without restarting the app.
            syncService.reloadConfig();
            log.info("Device registered and sync enabled");
            return ResponseEntity.ok(Map.of(
                    "success", true, "message", "Device registered. Syncing is now active."));
        }

        return ResponseEntity.status(400).body(Map.of(
                "success", false, "message", result.getMessage()));
    }

    @lombok.Data
    public static class RegisterRequest {
        private String email;
        private String registrationToken;
    }
}
