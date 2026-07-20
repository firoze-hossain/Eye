// src/main/java/com/trackeye/security/ApiKeyService.java
package com.roze.trackeyecentral.security;

import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final CryptoService cryptoService;

    /**
     * Generate a new API Key for a device
     * Format: tk_{encrypted_org_id}_{encrypted_user_id}_{random}
     */
    public String generateApiKey(Long userId, Long organizationId, String deviceIdentifier) {
        try {
            // Encrypt the IDs for additional security
            String encryptedOrgId = cryptoService.encrypt(String.valueOf(organizationId));
            String encryptedUserId = cryptoService.encrypt(String.valueOf(userId));
            
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            
            String apiKey = String.format("tk_%s_%s_%s", encryptedOrgId, encryptedUserId, randomPart);
            
            // Update device with new API key
            Device device = deviceRepository.findByDeviceIdentifier(deviceIdentifier)
                .orElseThrow(() -> new RuntimeException("Device not found"));
            
            device.setApiKey(apiKey);
            device.setApiKeyCreatedAt(Instant.now().toEpochMilli());
            deviceRepository.save(device);
            
            log.info("Generated API key for device: {} (user: {})", deviceIdentifier, userId);
            return apiKey;
            
        } catch (Exception e) {
            log.error("Failed to generate API key: {}", e.getMessage());
            throw new RuntimeException("Failed to generate API key", e);
        }
    }

    /**
     * Validate API Key and Device combination
     */
    @Transactional
    public ApiKeyValidationResult validateApiKey(String apiKey, String deviceId) {
        try {
            // Check format
            if (!apiKey.startsWith("tk_")) {
                return ApiKeyValidationResult.invalid("Invalid API key format");
            }

            // Find device by identifier
            Device device = deviceRepository.findByDeviceIdentifier(deviceId).orElse(null);
            if (device == null) {
                return ApiKeyValidationResult.invalid("Device not registered");
            }

            // Verify API key matches
            if (!apiKey.equals(device.getApiKey())) {
                return ApiKeyValidationResult.invalid("API key does not match this device");
            }

            // Check if API key is expired (365 days validity)
            if (device.getApiKeyCreatedAt() != null) {
                long expiryTime = device.getApiKeyCreatedAt() + (365L * 24 * 60 * 60 * 1000);
                if (System.currentTimeMillis() > expiryTime) {
                    return ApiKeyValidationResult.invalid("API key has expired");
                }
            }

            // Check if device is active
            if (!Boolean.TRUE.equals(device.getIsActive())) {
                return ApiKeyValidationResult.invalid("Device is deactivated");
            }

            // Get user
            User user = userRepository.findById(device.getUserId()).orElse(null);
            if (user == null) {
                return ApiKeyValidationResult.invalid("User not found");
            }

            // Check if user is active
            if (!"active".equals(user.getStatus())) {
                return ApiKeyValidationResult.invalid("User account is not active");
            }

            // Update last seen
            device.setLastSeenAt(System.currentTimeMillis());
            deviceRepository.save(device);

            return ApiKeyValidationResult.valid(user.getOrganizationId(), user.getId(), device.getId());

        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage());
            return ApiKeyValidationResult.invalid("Validation error: " + e.getMessage());
        }
    }

    /**
     * Revoke API Key for a device
     */
    @Transactional
    public void revokeApiKey(String deviceId) {
        Device device = deviceRepository.findByDeviceIdentifier(deviceId).orElse(null);
        if (device != null) {
            device.setApiKey(null);
            device.setApiKeyCreatedAt(null);
            device.setIsActive(false);
            deviceRepository.save(device);
            log.info("Revoked API key for device: {}", deviceId);
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor(staticName = "of")
    @lombok.NoArgsConstructor
    public static class ApiKeyValidationResult {
        private boolean valid;
        private Long organizationId;
        private Long userId;
        private Long deviceId;
        private String errorMessage;

        public static ApiKeyValidationResult valid(Long orgId, Long userId, Long deviceId) {
            return new ApiKeyValidationResult(true, orgId, userId, deviceId, null);
        }

        public static ApiKeyValidationResult invalid(String error) {
            return new ApiKeyValidationResult(false, null, null, null, error);
        }
    }
}