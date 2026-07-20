package com.roze.trackeyecentral.controller;

import com.roze.trackeyecentral.model.Notification;
import com.roze.trackeyecentral.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Notifications for the CALLER (whichever admin/supervisor is logged in) -
 * never org-wide, since a supervisor should only see alerts about their own
 * team (NotificationService already fans out per-recipient at creation time).
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> list(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(notificationService.listFor(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestAttribute("userId") Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(userId)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @RequestAttribute("userId") Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@RequestAttribute("userId") Long userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
