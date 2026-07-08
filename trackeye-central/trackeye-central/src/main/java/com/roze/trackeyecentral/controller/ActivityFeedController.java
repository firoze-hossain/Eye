// src/main/java/com/roze/trackeyecentral/controller/ActivityFeedController.java
package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.model.EmployeeActivity;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.ActivityRepository;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Org-wide activity feed for the Live Activity page: a flat, most-recent-first
 * list of tracked app sessions across every device in the organization.
 *
 * Uses the existing JPQL query (findByDeviceIdsAndTimeRange) - entities, not a
 * native projection - so there's no column-name/case mismatch to trip over.
 *
 * Lives under /api/admin/**, so UserAuthenticationFilter validates the Bearer
 * token and sets organizationId before this runs.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ActivityFeedController {

    private final ActivityRepository activityRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    @GetMapping("/activities")
    public ResponseEntity<List<Map<String, Object>>> feed(
            @RequestAttribute Long organizationId,
            @RequestParam(defaultValue = "200") int limit,
            @RequestParam(defaultValue = "168") int hours) {   // default: last 7 days

        List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
        if (devices.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Map<Long, Device> deviceById = devices.stream()
                .collect(Collectors.toMap(Device::getId, d -> d, (a, b) -> a));
        List<Long> deviceIds = new ArrayList<>(deviceById.keySet());

        long now = System.currentTimeMillis();
        long start = now - (long) hours * 60 * 60 * 1000;

        // Already ordered by startTime DESC by the query.
        List<EmployeeActivity> activities =
                activityRepository.findByDeviceIdsAndTimeRange(deviceIds, start, now);

        Map<Long, String> userNameCache = new HashMap<>();
        List<Map<String, Object>> out = new ArrayList<>();

        for (EmployeeActivity a : activities.stream().limit(Math.max(1, limit)).toList()) {
            Device d = deviceById.get(a.getDeviceId());
            String deviceName = (d != null) ? d.getDeviceName() : "Unknown";
            String userName = "Unknown";
            if (d != null) {
                userName = userNameCache.computeIfAbsent(d.getUserId(),
                        uid -> userRepository.findById(uid).map(User::getFullName).orElse("Unknown"));
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appName", a.getAppName());
            m.put("windowTitle", a.getWindowTitle());
            m.put("processName", a.getProcessName());
            m.put("startTime", a.getStartTime());
            m.put("endTime", a.getEndTime());
            m.put("durationMs", a.getDurationMs());
            m.put("userFullName", userName);
            m.put("deviceName", deviceName);
            out.add(m);
        }

        return ResponseEntity.ok(out);
    }
}
