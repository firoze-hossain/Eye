package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.service.UserService;
import com.roze.trackeyecentral.service.WatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin/manager side of "Watch Live". See WatchService for how the near-live
 * transport works. Every call here re-checks that the caller's role can see
 * the device's owner, so a supervisor can only watch their own team's screens.
 */
@RestController
@RequestMapping("/api/admin/devices")
@RequiredArgsConstructor
public class WatchController {

    private final WatchService watchService;
    private final DeviceRepository deviceRepository;
    private final UserService userService;

    @PostMapping("/{deviceId}/watch/start")
    public ResponseEntity<?> start(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        var guard = assertVisible(organizationId, callerUserId, role, deviceId);
        if (guard != null) return guard;
        watchService.start(deviceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{deviceId}/watch/renew")
    public ResponseEntity<?> renew(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        var guard = assertVisible(organizationId, callerUserId, role, deviceId);
        if (guard != null) return guard;
        watchService.renew(deviceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{deviceId}/watch/stop")
    public ResponseEntity<?> stop(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        var guard = assertVisible(organizationId, callerUserId, role, deviceId);
        if (guard != null) return guard;
        watchService.stop(deviceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping(value = "/{deviceId}/watch/frame", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> frame(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null || !userService.canView(organizationId, callerUserId, role, device.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        byte[] frame = watchService.latestFrame(deviceId);
        if (frame == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(frame);
    }

    private ResponseEntity<?> assertVisible(Long organizationId, Long callerUserId, String role, Long deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return ResponseEntity.notFound().build();
        if (!userService.canView(organizationId, callerUserId, role, device.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        return null;
    }
}
