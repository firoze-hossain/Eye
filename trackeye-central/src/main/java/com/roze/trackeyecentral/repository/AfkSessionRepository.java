// src/main/java/com/trackeye/repository/AfkSessionRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.AfkSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AfkSessionRepository extends JpaRepository<AfkSession, Long> {

    List<AfkSession> findByDeviceIdOrderByStartTimeDesc(Long deviceId);

    List<AfkSession> findByDeviceIdAndStartTimeBetween(Long deviceId, Long startTime, Long endTime);

    @Query("SELECT a FROM AfkSession a WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime ORDER BY a.startTime DESC")
    List<AfkSession> findByDeviceIdsAndTimeRange(@Param("deviceIds") List<Long> deviceIds,
                                                 @Param("startTime") Long startTime,
                                                 @Param("endTime") Long endTime);

    @Query("SELECT COALESCE(SUM(a.durationMs), 0) FROM AfkSession a " +
            "WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime")
    Long getTotalAfkTimeForDevices(@Param("deviceIds") List<Long> deviceIds,
                                   @Param("startTime") Long startTime,
                                   @Param("endTime") Long endTime);

    @Query("SELECT COUNT(a) FROM AfkSession a WHERE a.deviceId = :deviceId AND a.startTime BETWEEN :startTime AND :endTime")
    Long countAfkSessionsForDevice(@Param("deviceId") Long deviceId,
                                   @Param("startTime") Long startTime,
                                   @Param("endTime") Long endTime);

    @Query("SELECT AVG(a.durationMs) FROM AfkSession a WHERE a.deviceId = :deviceId AND a.startTime BETWEEN :startTime AND :endTime")
    Long getAverageAfkDuration(@Param("deviceId") Long deviceId,
                               @Param("startTime") Long startTime,
                               @Param("endTime") Long endTime);

    // Fixed version - Using native query for H2 compatibility
    @Query(value = "SELECT EXTRACT(HOUR FROM TIMESTAMP 'epoch' + (a.start_time / 1000) * INTERVAL '1 second') as hour, " +
            "COUNT(a.id) as session_count, " +
            "SUM(a.duration_ms) as total_ms " +
            "FROM employee_afk_sessions a " +
            "WHERE a.device_id IN (:deviceIds) " +
            "AND a.start_time BETWEEN :startTime AND :endTime " +
            "GROUP BY EXTRACT(HOUR FROM TIMESTAMP 'epoch' + (a.start_time / 1000) * INTERVAL '1 second')",
            nativeQuery = true)
    List<Map<String, Object>> getAfkPatternByHour(@Param("deviceIds") List<Long> deviceIds,
                                                  @Param("startTime") Long startTime,
                                                  @Param("endTime") Long endTime);

    // Alternative simpler version without hour extraction (for compatibility)
    @Query("SELECT a FROM AfkSession a WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime")
    List<AfkSession> getAfkSessionsForTimeRange(@Param("deviceIds") List<Long> deviceIds,
                                                @Param("startTime") Long startTime,
                                                @Param("endTime") Long endTime,
                                                Pageable pageable);

    @Query(value = "SELECT a.start_time, a.end_time, a.duration_ms, " +
            "u.full_name as user_name " +
            "FROM employee_afk_sessions a " +
            "JOIN devices d ON a.device_id = d.id " +
            "JOIN users u ON d.user_id = u.id " +
            "WHERE u.organization_id = :organizationId " +
            "AND a.start_time BETWEEN :startTime AND :endTime " +
            "ORDER BY a.start_time DESC", nativeQuery = true)
    List<Map<String, Object>> getOrganizationAfkSessions(@Param("organizationId") Long organizationId,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime);
}