// src/main/java/com/trackeye/dto/response/SyncResponse.java
package com.roze.trackeyecentral.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyncResponse {
    private boolean success;
    private int recordsSaved;
    private long lastSyncTimestamp;
    private String errorMessage;
}