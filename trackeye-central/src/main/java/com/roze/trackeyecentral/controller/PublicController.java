package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.dto.RegistrationResponse;
import com.roze.trackeyecentral.service.OrganizationService;
import com.roze.trackeyecentral.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final OrganizationService organizationService;
    private final UserService userService;

    // Map both /api/public/register and /api/backend/public/register
    @PostMapping("/public/register")
    public ResponseEntity<RegistrationResponse> registerOrganization(@Valid @RequestBody OrganizationRegistrationRequest request) {
        log.info("New organization registration: {}", request.getOrgName());
        RegistrationResponse response = organizationService.registerOrganization(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/health")
    public ResponseEntity<Object> health() {
        return ResponseEntity.ok().body(new HealthResponse("OK", System.currentTimeMillis()));
    }

    @PostMapping("/public/register-device")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody DeviceRegistrationRequest request) {
        log.info("Device registration request for email: {}", request.getEmail());
        try {
            RegistrationResponse response = userService.registerDevice(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // FIX: this used to have no try/catch, so any failure (wrong token
            // type, expired token, decrypt failure, user not found) propagated
            // as a raw exception -> Spring's default handler -> a generic,
            // useless "500 Internal Server Error" with no message. The agent
            // could only ever show "Server error: 500 - {...}" with nothing
            // actionable in it. Now the real reason reaches the person trying
            // to register.
            log.warn("Device registration failed: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Registration failed"));
        }
    }

    @GetMapping("/public/verify-invite")
    public ResponseEntity<InviteVerificationResponse> verifyInvite(@RequestParam String token) {
        InviteVerificationResponse response = userService.verifyInviteToken(token);
        return ResponseEntity.ok(response);
    }

    @lombok.Data
    public static class HealthResponse {
        private final String status;
        private final long timestamp;
    }

    @lombok.Data
    public static class OrganizationRegistrationRequest {
        @jakarta.validation.constraints.NotBlank
        private String orgName;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String adminEmail;

        @jakarta.validation.constraints.NotBlank
        private String adminFullName;

        @jakarta.validation.constraints.NotBlank
        private String password;

        private String subdomain;
        private String planType = "basic";
    }

    @lombok.Data
    public static class DeviceRegistrationRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String email;

        @jakarta.validation.constraints.NotBlank
        private String registrationToken;

        @jakarta.validation.constraints.NotBlank
        private String deviceId;

        @jakarta.validation.constraints.NotBlank
        private String deviceName;

        private String osType;
    }

    @lombok.Data
    public static class InviteVerificationResponse {
        private boolean valid;
        private String organizationName;
        private String userEmail;
        private String userFullName;
        private String setupInstructions;
        private String serverUrl;
    }
}