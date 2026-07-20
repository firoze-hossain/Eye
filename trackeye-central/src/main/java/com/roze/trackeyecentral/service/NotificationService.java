package com.roze.trackeyecentral.service;

import com.roze.trackeyecentral.model.Notification;
import com.roze.trackeyecentral.model.User;
import com.roze.trackeyecentral.repository.NotificationRepository;
import com.roze.trackeyecentral.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public List<Notification> listFor(Long userId) {
        return notificationRepository.findByRecipient(userId);
    }

    public long unreadCount(Long userId) {
        return notificationRepository.countUnread(userId);
    }

    public void markRead(Long userId, Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getRecipientUserId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllRead(Long userId) {
        notificationRepository.findByRecipient(userId).forEach(n -> {
            if (!n.getRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    /**
     * Sends this notification to every admin in the org, plus the employee's own
     * manager if one is assigned (and isn't already an admin, to avoid duplicates).
     */
    public void notifyAdminsAndManager(Long organizationId, Long subjectUserId,
                                        String type, String title, String body) {
        List<User> admins = userRepository.findByOrganizationIdAndRole(organizationId, "admin");
        for (User admin : admins) {
            create(organizationId, admin.getId(), subjectUserId, type, title, body);
        }

        User subject = userRepository.findById(subjectUserId).orElse(null);
        if (subject != null && subject.getManagerId() != null) {
            boolean managerIsAdmin = admins.stream().anyMatch(a -> a.getId().equals(subject.getManagerId()));
            if (!managerIsAdmin) {
                create(organizationId, subject.getManagerId(), subjectUserId, type, title, body);
            }
        }
    }

    private void create(Long organizationId, Long recipientUserId, Long subjectUserId,
                         String type, String title, String body) {
        Notification n = new Notification();
        n.setOrganizationId(organizationId);
        n.setRecipientUserId(recipientUserId);
        n.setSubjectUserId(subjectUserId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setRead(false);
        n.setCreatedAt(System.currentTimeMillis());
        notificationRepository.save(n);
    }
}
