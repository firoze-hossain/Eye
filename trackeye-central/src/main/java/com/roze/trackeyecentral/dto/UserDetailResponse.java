// src/main/java/com/trackeye/dto/response/UserDetailResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private Long createdAt;
    private Long lastLoginAt;
    private List<DeviceResponse> devices;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceResponse {
        private Long id;
        private String deviceName;
        private String deviceIdentifier;
        private String osType;
        private Long lastSeenAt;
        private Boolean isActive;
        private Long createdAt;
    }
}