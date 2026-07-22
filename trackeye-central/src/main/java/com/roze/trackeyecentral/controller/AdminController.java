// src/main/java/com/trackeye/controller/AdminController.java
package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.dto.*;
import com.roze.trackeyecentral.model.Device;
import com.roze.trackeyecentral.repository.DeviceRepository;
import com.roze.trackeyecentral.service.OrganizationService;
import com.roze.trackeyecentral.service.ReportService;
import com.roze.trackeyecentral.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrganizationService organizationService;
    private final UserService userService;
    private final DeviceRepository deviceRepository;
    private final ReportService reportService;

    /**
     * Get dashboard statistics for organization
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@RequestAttribute Long organizationId) {
        DashboardResponse response = reportService.getDashboardStats(organizationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get employees visible to the caller: admins see the whole organization,
     * supervisors see only the employees assigned to them.
     */
    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getEmployees(
            @RequestAttribute Long organizationId,
            @RequestAttribute Long userId,
            @RequestAttribute String role) {
        List<UserResponse> employees = userService.getEmployeesScoped(organizationId, userId, role);
        return ResponseEntity.ok(employees);
    }

    /** Admin-only: assign or clear (managerId=null) an employee's manager. */
    @PostMapping("/employees/{employeeId}/manager")
    public ResponseEntity<Map<String, Object>> assignManager(
            @RequestAttribute Long organizationId,
            @RequestAttribute String role,
            @PathVariable Long employeeId,
            @RequestBody Map<String, Long> body) {
        if (!"admin".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Only admins can assign managers"));
        }
        userService.assignManager(organizationId, employeeId, body.get("managerId"));
        return ResponseEntity.ok(Map.of("success", true));
    }

//    /** Admin-only: assign or clear (managerId=null) an employee's manager. */
//    @PostMapping("/employees/{employeeId}/manager")
//    public ResponseEntity<Map<String, Object>> assignManager(
//            @RequestAttribute Long organizationId,
//            @RequestAttribute String role,
//            @PathVariable Long employeeId,
//            @RequestBody Map<String, Long> body) {
//        if (!"admin".equalsIgnoreCase(role)) {
//            return ResponseEntity.status(403).body(Map.of("error", "Only admins can assign managers"));
//        }
//        userService.assignManager(organizationId, employeeId, body.get("managerId"));
//        return ResponseEntity.ok(Map.of("success", true));
//    }

    /**
     * Get employee details
     */
    @GetMapping("/employees/{userId}")
    public ResponseEntity<?> getEmployeeDetails(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long userId) {
        if (!userService.canView(organizationId, callerUserId, role, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        UserDetailResponse employee = userService.getEmployeeDetails(organizationId, userId);
        return ResponseEntity.ok(employee);
    }

    /**
     * Get employee activities for a specific date
     */
    @GetMapping("/employees/{userId}/activities")
    public ResponseEntity<?> getEmployeeActivities(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long userId,
            @RequestParam String date) {
        if (!userService.canView(organizationId, callerUserId, role, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        LocalDate localDate = LocalDate.parse(date);
        EmployeeActivityResponse activities = reportService.getEmployeeActivities(
            organizationId, userId, localDate);
        return ResponseEntity.ok(activities);
    }

    /**
     * Get employee screenshots for a specific date
     */
    @GetMapping("/employees/{userId}/screenshots")
    public ResponseEntity<?> getEmployeeScreenshots(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long userId,
            @RequestParam String date) {
        if (!userService.canView(organizationId, callerUserId, role, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        LocalDate localDate = LocalDate.parse(date);
        List<ScreenshotResponse> screenshots = reportService.getEmployeeScreenshots(
            organizationId, userId, localDate);
        return ResponseEntity.ok(screenshots);
    }

    /**
     * Get employee browser activity (URLs visited) for a specific date - its
     * own section so "which sites did they visit" is easy to see clearly,
     * separate from general app activity.
     */
    @GetMapping("/employees/{userId}/browser-activities")
    public ResponseEntity<?> getEmployeeBrowserActivities(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long userId,
            @RequestParam String date) {
        if (!userService.canView(organizationId, callerUserId, role, userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        LocalDate localDate = LocalDate.parse(date);
        List<BrowserActivityResponse> browserActivities = reportService.getEmployeeBrowserActivities(
            organizationId, userId, localDate);
        return ResponseEntity.ok(browserActivities);
    }

    /**
     * Get real-time active users
     */
    @GetMapping("/live")
    public ResponseEntity<List<LiveActivityResponse>> getLiveActivities(@RequestAttribute Long organizationId) {
        List<LiveActivityResponse> liveActivities = reportService.getLiveActivities(organizationId);
        return ResponseEntity.ok(liveActivities);
    }

    /**
     * Get weekly report
     */
    @GetMapping("/reports/weekly")
    public ResponseEntity<WeeklyReportResponse> getWeeklyReport(
            @RequestAttribute Long organizationId,
            @RequestParam(required = false) Long userId) {
        
        WeeklyReportResponse report = reportService.getWeeklyReport(organizationId, userId);
        return ResponseEntity.ok(report);
    }

    /**
     * Invite new employee
     */
    @PostMapping("/invite")
    public ResponseEntity<InviteResponse> inviteEmployee(
            @RequestAttribute Long organizationId,
            @RequestAttribute Long userId,
            @Valid @RequestBody InviteRequest request) {
        
        InviteResponse response = userService.inviteEmployee(organizationId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate employee
     */
    @PostMapping("/employees/{userId}/deactivate")
    public ResponseEntity<Void> deactivateEmployee(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId) {
        
        userService.deactivateEmployee(organizationId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Activate employee
     */
    @PostMapping("/employees/{userId}/activate")
    public ResponseEntity<Void> activateEmployee(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId) {
        
        userService.activateEmployee(organizationId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Revoke device access
     */
    @PostMapping("/devices/{deviceId}/revoke")
    public ResponseEntity<Void> revokeDevice(
            @RequestAttribute Long organizationId,
            @PathVariable Long deviceId) {
        
        userService.revokeDevice(organizationId, deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * Pause a device's syncing (reversible - no new registration token needed
     * to resume). A supervisor may only pause/resume devices belonging to
     * their own team; an admin may pause any device in the organization,
     * including their own.
     */
    @PostMapping("/devices/{deviceId}/pause")
    public ResponseEntity<?> pauseDevice(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        var guard = assertDeviceVisible(organizationId, callerUserId, role, deviceId);
        if (guard != null) return guard;
        userService.pauseDevice(organizationId, deviceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/devices/{deviceId}/resume")
    public ResponseEntity<?> resumeDevice(
            @RequestAttribute Long organizationId,
            @RequestAttribute("userId") Long callerUserId,
            @RequestAttribute String role,
            @PathVariable Long deviceId) {
        var guard = assertDeviceVisible(organizationId, callerUserId, role, deviceId);
        if (guard != null) return guard;
        userService.resumeDevice(organizationId, deviceId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    private ResponseEntity<?> assertDeviceVisible(Long organizationId, Long callerUserId, String role, Long deviceId) {
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device == null) return ResponseEntity.notFound().build();
        if (!userService.canManage(organizationId, callerUserId, role, device.getUserId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Not visible to your role"));
        }
        return null;
    }

}