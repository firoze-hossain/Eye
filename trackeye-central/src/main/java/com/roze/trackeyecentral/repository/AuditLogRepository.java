// src/main/java/com/trackeye/repository/AuditLogRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<AuditLog> findByOrganizationIdAndActionOrderByCreatedAtDesc(Long organizationId, String action);
    
    @Query("SELECT a FROM AuditLog a WHERE a.organizationId = :organizationId AND a.createdAt BETWEEN :startTime AND :endTime ORDER BY a.createdAt DESC")
    List<AuditLog> findByOrganizationIdAndTimeRange(@Param("organizationId") Long organizationId,
                                                     @Param("startTime") Long startTime,
                                                     @Param("endTime") Long endTime);
    
    @Query("SELECT a.action, COUNT(a) as count FROM AuditLog a WHERE a.organizationId = :organizationId GROUP BY a.action")
    List<Object[]> getActionCounts(@Param("organizationId") Long organizationId);
}