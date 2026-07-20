// src/main/java/com/trackeye/dto/response/RegistrationResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponse {
    private Long organizationId;
    private String organizationName;
    private String subdomain;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String registrationToken;
    private Long deviceId;
    private String apiKey;
    private String deviceIdentifier;
    private String serverUrl;
    private String message;
}