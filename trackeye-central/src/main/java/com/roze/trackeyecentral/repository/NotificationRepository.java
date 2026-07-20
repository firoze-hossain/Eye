package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipientUserId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByRecipient(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientUserId = :userId AND n.read = false")
    long countUnread(@Param("userId") Long userId);
}
