// src/main/java/com/trackeye/service/UserService.java
package com.roze.trackeyecentral.service;

import com.roze.trackeyecentral.controller.PublicController;
import com.roze.trackeyecentral.dto.*;
import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import com.roze.trackeyecentral.security.ApiKeyService;
import com.roze.trackeyecentral.security.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final ApiKeyService apiKeyService;
    private final CryptoService cryptoService;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public RegistrationResponse registerDevice(PublicController.DeviceRegistrationRequest request) {
        
        // Validate registration token
        String decryptedToken = cryptoService.decrypt(request.getRegistrationToken());
        String[] parts = decryptedToken.split(":");
        if (parts.length < 2) {
            throw new RuntimeException("Invalid registration token");
        }
        
        Long userId = Long.parseLong(parts[0]);
        Long orgId = Long.parseLong(parts[1]);
        
        // Find user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getOrganizationId().equals(orgId)) {
            throw new RuntimeException("Invalid token for this user");
        }
        
        // Check if device already exists
        Device existingDevice = deviceRepository.findByDeviceIdentifier(request.getDeviceId()).orElse(null);
        if (existingDevice != null) {
            // Reactivate existing device
            existingDevice.setIsActive(true);
            existingDevice.setDeviceName(request.getDeviceName());
            existingDevice.setOsType(request.getOsType());
            existingDevice.setLastSeenAt(Instant.now().toEpochMilli());
            deviceRepository.save(existingDevice);
            
            // Generate new API key
            String apiKey = apiKeyService.generateApiKey(userId, orgId, request.getDeviceId());
            
            return RegistrationResponse.builder()
                .organizationId(orgId)
                .userId(userId)
                .deviceId(existingDevice.getId())
                .apiKey(apiKey)
                .deviceIdentifier(request.getDeviceId())
                .serverUrl(baseUrl)
                .message("Device re-registered successfully")
                .build();
        }
        
        // Create new device
        Device device = new Device();
        device.setUserId(userId);
        device.setDeviceName(request.getDeviceName());
        device.setDeviceIdentifier(request.getDeviceId());
        device.setOsType(request.getOsType());
        device.setCreatedAt(Instant.now().toEpochMilli());
        device.setIsActive(true);
        
        device = deviceRepository.save(device);
        
        // Generate API key
        String apiKey = apiKeyService.generateApiKey(userId, orgId, request.getDeviceId());
        
        log.info("Registered new device: {} for user: {}", request.getDeviceName(), user.getEmail());
        
        return RegistrationResponse.builder()
            .organizationId(orgId)
            .userId(userId)
            .deviceId(device.getId())
            .apiKey(apiKey)
            .deviceIdentifier(request.getDeviceId())
            .serverUrl(baseUrl)
            .message("Device registered successfully")
            .build();
    }
    
    public List<UserResponse> getEmployeesByOrganization(Long organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }
    
    public UserDetailResponse getEmployeeDetails(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("User does not belong to this organization");
        }
        
        List<Device> devices = deviceRepository.findByUserId(userId);
        
        return UserDetailResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .devices(devices.stream().map(this::toDeviceResponse).collect(Collectors.toList()))
            .build();
    }
    
    @Transactional
    public InviteResponse inviteEmployee(Long organizationId, Long invitedByUserId, InviteRequest request) {
        
        // Check user limit
        long activeUsers = userRepository.countActiveUsersByOrganizationId(organizationId);
        // In production, check against plan limits
        
        // Create user
        User user = new User();
        user.setOrganizationId(organizationId);
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setStatus("invited");
        user.setCreatedAt(Instant.now().toEpochMilli());
        user.setInvitedBy(invitedByUserId);
        
        userRepository.save(user);
        
        // Generate invite token
        String inviteToken = generateInviteToken(user.getId(), organizationId);
        
        String inviteLink = baseUrl + "/invite?token=" + inviteToken;
        
        log.info("Invited employee: {} to organization: {}", request.getEmail(), organizationId);
        
        return InviteResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .inviteLink(inviteLink)
            .inviteToken(inviteToken)
            .expiresInHours(48)
            .message("Invitation sent successfully")
            .build();
    }
    
    private String generateInviteToken(Long userId, Long orgId) {
        String rawToken = "invite:" + userId + ":" + orgId + ":" + System.currentTimeMillis();
        return cryptoService.encrypt(rawToken);
    }
    
    public PublicController.InviteVerificationResponse verifyInviteToken(String token) {
        try {
            String decrypted = cryptoService.decrypt(token);
            if (!decrypted.startsWith("invite:")) {
                return createInvalidResponse();
            }
            
            String[] parts = decrypted.split(":");
            if (parts.length < 4) {
                return createInvalidResponse();
            }
            
            Long userId = Long.parseLong(parts[1]);
            Long orgId = Long.parseLong(parts[2]);
            long createdAt = Long.parseLong(parts[3]);
            
            // Check if expired (48 hours)
            if (System.currentTimeMillis() - createdAt > 48 * 60 * 60 * 1000) {
                return createExpiredResponse();
            }
            
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.getOrganizationId().equals(orgId)) {
                return createInvalidResponse();
            }
            
            PublicController.InviteVerificationResponse response = new PublicController.InviteVerificationResponse();
            response.setValid(true);
            response.setOrganizationName("Organization"); // Fetch from org
            response.setUserEmail(user.getEmail());
            response.setUserFullName(user.getFullName());
            response.setServerUrl(baseUrl);
            response.setSetupInstructions("Download the TrackEye desktop app and use the registration token below to set up your device.");
            
            return response;
            
        } catch (Exception e) {
            log.error("Error verifying invite token: {}", e.getMessage());
            return createInvalidResponse();
        }
    }
    
    private PublicController.InviteVerificationResponse createInvalidResponse() {
        PublicController.InviteVerificationResponse response = new PublicController.InviteVerificationResponse();
        response.setValid(false);
        return response;
    }
    
    private PublicController.InviteVerificationResponse createExpiredResponse() {
        PublicController.InviteVerificationResponse response = new PublicController.InviteVerificationResponse();
        response.setValid(false);
        response.setSetupInstructions("This invitation has expired. Please contact your administrator for a new invitation.");
        return response;
    }
    
    @Transactional
    public void deactivateEmployee(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("User does not belong to this organization");
        }
        
        user.setStatus("suspended");
        userRepository.save(user);
        
        // Deactivate all devices
        List<Device> devices = deviceRepository.findByUserId(userId);
        for (Device device : devices) {
            device.setIsActive(false);
            deviceRepository.save(device);
        }
        
        log.info("Deactivated employee: {} in organization: {}", user.getEmail(), organizationId);
    }
    
    @Transactional
    public void activateEmployee(Long organizationId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("User does not belong to this organization");
        }
        
        user.setStatus("active");
        userRepository.save(user);
        
        log.info("Activated employee: {} in organization: {}", user.getEmail(), organizationId);
    }
    
    @Transactional
    public void revokeDevice(Long organizationId, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new RuntimeException("Device not found"));
        
        User user = userRepository.findById(device.getUserId()).orElse(null);
        if (user == null || !user.getOrganizationId().equals(organizationId)) {
            throw new RuntimeException("Device does not belong to this organization");
        }
        
        device.setIsActive(false);
        device.setApiKey(null);
        deviceRepository.save(device);
        
        log.info("Revoked device: {} for user: {}", device.getDeviceName(), user.getEmail());
    }
    
    private UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLoginAt(user.getLastLoginAt());
        return response;
    }
    
    private UserDetailResponse.DeviceResponse toDeviceResponse(Device device) {
        UserDetailResponse.DeviceResponse response = new UserDetailResponse.DeviceResponse();
        response.setId(device.getId());
        response.setDeviceName(device.getDeviceName());
        response.setDeviceIdentifier(device.getDeviceIdentifier());
        response.setOsType(device.getOsType());
        response.setLastSeenAt(device.getLastSeenAt());
        response.setIsActive(device.getIsActive());
        response.setCreatedAt(device.getCreatedAt());
        return response;
    }
}