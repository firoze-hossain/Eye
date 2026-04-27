// src/main/java/com/trackeye/service/OrganizationService.java
package com.roze.trackeyecentral.service;

import com.roze.trackeyecentral.controller.PublicController;
import com.roze.trackeyecentral.dto.RegistrationResponse;
import com.roze.trackeyecentral.model.Organization;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.OrganizationRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import com.roze.trackeyecentral.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CryptoService cryptoService;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public RegistrationResponse registerOrganization(PublicController.OrganizationRegistrationRequest request) {
        
        // Generate subdomain if not provided
        String subdomain = request.getSubdomain();
        if (subdomain == null || subdomain.isEmpty()) {
            subdomain = request.getOrgName().toLowerCase().replaceAll("[^a-z0-9]", "-");
        }
        
        // Check if subdomain exists
        if (organizationRepository.existsBySubdomain(subdomain)) {
            subdomain = subdomain + "-" + UUID.randomUUID().toString().substring(0, 4);
        }
        
        // Create organization
        Organization organization = new Organization();
        organization.setOrgName(request.getOrgName());
        organization.setSubdomain(subdomain);
        organization.setPlanType(request.getPlanType());
        organization.setCreatedAt(Instant.now().toEpochMilli());
        organization.setUpdatedAt(Instant.now().toEpochMilli());
        
        organization = organizationRepository.save(organization);
        
        // Create admin user
        User admin = new User();
        admin.setOrganizationId(organization.getId());
        admin.setEmail(request.getAdminEmail());
        admin.setFullName(request.getAdminFullName());
        admin.setRole("admin");
        admin.setStatus("active");
        admin.setCreatedAt(Instant.now().toEpochMilli());
        
        // In production, hash the password properly
        admin.setPasswordHash(cryptoService.encrypt(request.getPassword()));
        
        userRepository.save(admin);
        
        log.info("Created new organization: {} (ID: {}) with admin: {}", 
            organization.getOrgName(), organization.getId(), admin.getEmail());
        
        // Generate temporary registration token
        String tempToken = generateTempToken(admin.getId(), organization.getId());
        
        return RegistrationResponse.builder()
            .organizationId(organization.getId())
            .organizationName(organization.getOrgName())
            .subdomain(organization.getSubdomain())
            .userId(admin.getId())
            .userEmail(admin.getEmail())
            .userFullName(admin.getFullName())
            .registrationToken(tempToken)
            .serverUrl(baseUrl)
            .message("Organization registered successfully. Please download the desktop app and use this token to register your device.")
            .build();
    }
    
    private String generateTempToken(Long userId, Long orgId) {
        // Simple token generation - in production, use JWT or store in database
        String rawToken = userId + ":" + orgId + ":" + System.currentTimeMillis();
        return cryptoService.encrypt(rawToken);
    }
}