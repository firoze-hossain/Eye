// Add to your existing desktop app
package com.roze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service
public class InstallationService {

    @Value("${trackeye.server.url:http://localhost:8080}")
    private String serverUrl;
    
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.trackeye/config.properties";
    
    /**
     * Register this device with the central server
     */
    public RegistrationResult registerDevice(String email, String registrationToken) {
        try {
            String deviceId = getOrCreateDeviceId();
            String deviceName = getDeviceName();
            String osType = System.getProperty("os.name");
            
            RegistrationRequest request = new RegistrationRequest();
            request.setEmail(email);
            request.setRegistrationToken(registrationToken);
            request.setDeviceId(deviceId);
            request.setDeviceName(deviceName);
            request.setOsType(osType);
            
            String jsonBody = objectMapper.writeValueAsString(request);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/api/public/register-device"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                RegistrationResponse resp = objectMapper.readValue(response.body(), RegistrationResponse.class);
                
                // Save configuration
                saveConfiguration(resp);
                
                log.info("Device registered successfully!");
                return RegistrationResult.success(resp);
            } else {
                log.error("Registration failed: {}", response.body());
                return RegistrationResult.failure("Server error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return RegistrationResult.failure(e.getMessage());
        }
    }
    
    private String getOrCreateDeviceId() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                Properties props = new Properties();
                props.load(Files.newInputStream(configPath));
                String existingId = props.getProperty("device.id");
                if (existingId != null && !existingId.isEmpty()) {
                    return existingId;
                }
            }
        } catch (Exception e) {
            log.warn("Could not read existing device ID: {}", e.getMessage());
        }
        
        // Generate new device ID based on MAC address or random
        try {
            java.net.NetworkInterface network = java.net.NetworkInterface.getNetworkInterfaces()
                    .nextElement();
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (byte b : mac) {
                sb.append(String.format("%02X", b));
            }
            return "dev_" + sb.toString();
        } catch (Exception e) {
            return "dev_" + UUID.randomUUID().toString();
        }
    }
    
    private String getDeviceName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "Unknown Device";
        }
    }
    
    private void saveConfiguration(RegistrationResponse response) throws Exception {
        Path configDir = Paths.get(System.getProperty("user.home"), ".trackeye");
        Files.createDirectories(configDir);
        
        Properties props = new Properties();
        props.setProperty("trackeye.server.url", serverUrl);
        props.setProperty("trackeye.client.api-key", response.getApiKey());
        props.setProperty("trackeye.client.device-id", response.getDeviceIdentifier());
        props.setProperty("trackeye.client.user-id", String.valueOf(response.getUserId()));
        props.setProperty("trackeye.client.org-id", String.valueOf(response.getOrganizationId()));
        props.setProperty("device.id", response.getDeviceIdentifier());
        props.setProperty("registered.at", String.valueOf(System.currentTimeMillis()));
        
        try (var out = Files.newOutputStream(configDir.resolve("config.properties"))) {
            props.store(out, "TrackEye Client Configuration");
        }
        
        log.info("Configuration saved to: {}", configDir);
    }
    
    @lombok.Data
    public static class RegistrationRequest {
        private String email;
        private String registrationToken;
        private String deviceId;
        private String deviceName;
        private String osType;
    }
    
    @lombok.Data
    public static class RegistrationResponse {
        private Long organizationId;
        private Long userId;
        private Long deviceId;
        private String apiKey;
        private String deviceIdentifier;
        private String serverUrl;
        private String message;
    }
    
    @lombok.Data
    public static class RegistrationResult {
        private boolean success;
        private String message;
        private RegistrationResponse data;
        
        public static RegistrationResult success(RegistrationResponse data) {
            RegistrationResult result = new RegistrationResult();
            result.setSuccess(true);
            result.setData(data);
            result.setMessage("Device registered successfully");
            return result;
        }
        
        public static RegistrationResult failure(String message) {
            RegistrationResult result = new RegistrationResult();
            result.setSuccess(false);
            result.setMessage(message);
            return result;
        }
    }
}