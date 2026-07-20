package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.service.WatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Device-side of "Watch Live" - lives under /api/sync/** so the existing
 * ApiKeyAuthenticationFilter authenticates it exactly like every other sync
 * call (X-API-Key + X-Device-ID headers), and deviceId is already resolved
 * onto the request by that filter.
 */
@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class WatchSyncController {

    private final WatchService watchService;

    /** The agent polls this cheaply and only starts capturing when it says active=true. */
    @GetMapping("/watch-status")
    public ResponseEntity<Map<String, Boolean>> status(@RequestAttribute Long deviceId) {
        return ResponseEntity.ok(Map.of("active", watchService.isActive(deviceId)));
    }

    @PostMapping(value = "/watch-frame", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> uploadFrame(
            @RequestAttribute Long deviceId,
            @RequestParam("file") MultipartFile file) {
        if (!watchService.isActive(deviceId)) {
            // Session ended between the agent's last status check and this frame -
            // accept it silently rather than erroring; the agent will stop next poll.
            return ResponseEntity.ok(Map.of("success", true, "active", false));
        }
        try {
            watchService.pushFrame(deviceId, file.getBytes());
            return ResponseEntity.ok(Map.of("success", true, "active", true));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
