// src/main/java/com/trackeye/controller/AdminController.java
package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.dto.*;
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
     * Get all employees in organization
     */
    @GetMapping("/employees")
    public ResponseEntity<List<UserResponse>> getEmployees(@RequestAttribute Long organizationId) {
        List<UserResponse> employees = userService.getEmployeesByOrganization(organizationId);
        return ResponseEntity.ok(employees);
    }

    /**
     * Get employee details
     */
    @GetMapping("/employees/{userId}")
    public ResponseEntity<UserDetailResponse> getEmployeeDetails(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId) {
        UserDetailResponse employee = userService.getEmployeeDetails(organizationId, userId);
        return ResponseEntity.ok(employee);
    }

    /**
     * Get employee activities for a specific date
     */
    @GetMapping("/employees/{userId}/activities")
    public ResponseEntity<EmployeeActivityResponse> getEmployeeActivities(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId,
            @RequestParam String date) {
        
        LocalDate localDate = LocalDate.parse(date);
        EmployeeActivityResponse activities = reportService.getEmployeeActivities(
            organizationId, userId, localDate);
        return ResponseEntity.ok(activities);
    }

    /**
     * Get employee screenshots for a specific date
     */
    @GetMapping("/employees/{userId}/screenshots")
    public ResponseEntity<List<ScreenshotResponse>> getEmployeeScreenshots(
            @RequestAttribute Long organizationId,
            @PathVariable Long userId,
            @RequestParam String date) {
        
        LocalDate localDate = LocalDate.parse(date);
        List<ScreenshotResponse> screenshots = reportService.getEmployeeScreenshots(
            organizationId, userId, localDate);
        return ResponseEntity.ok(screenshots);
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

}