// src/main/java/com/trackeye/dto/request/RegistrationRequest.java
package com.roze.trackeyecentral.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationRequest {
    @NotBlank
    private String orgName;
    
    @NotBlank
    @Email
    private String adminEmail;
    
    @NotBlank
    private String adminFullName;
    
    @NotBlank
    private String password;
    
    private String subdomain;
    private String planType = "basic";
}