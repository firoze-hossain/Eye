package com.roze.trackeyecentral.dto;

@lombok.Data
    public  class AfkSessionSyncRequest {
        private Long startTime;
        private Long endTime;
        private Long durationMs;
    }