// src/main/java/com/trackeye/dto/response/InviteResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteResponse {
    private Long userId;
    private String email;
    private String inviteLink;
    private String inviteToken;
    private int expiresInHours;
    private String message;
}