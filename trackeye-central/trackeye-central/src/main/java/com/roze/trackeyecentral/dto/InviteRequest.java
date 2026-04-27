package com.roze.trackeyecentral.dto;

@lombok.Data
    public  class InviteRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String email;
        
        @jakarta.validation.constraints.NotBlank
        private String fullName;
        
        private String role = "employee";
    }