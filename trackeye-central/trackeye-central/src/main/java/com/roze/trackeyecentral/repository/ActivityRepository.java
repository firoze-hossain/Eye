// src/main/java/com/trackeye/repository/ActivityRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.EmployeeActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface ActivityRepository extends JpaRepository<EmployeeActivity, Long> {
    
    List<EmployeeActivity> findByDeviceIdOrderByStartTimeDesc(Long deviceId);
    
    List<EmployeeActivity> findByDeviceIdAndStartTimeBetween(Long deviceId, Long startTime, Long endTime);
    
    @Query("SELECT a FROM EmployeeActivity a WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime ORDER BY a.startTime DESC")
    List<EmployeeActivity> findByDeviceIdsAndTimeRange(@Param("deviceIds") List<Long> deviceIds, 
                                                        @Param("startTime") Long startTime, 
                                                        @Param("endTime") Long endTime);
    
    @Query("SELECT a.appName as appName, SUM(a.durationMs) as totalMs, COUNT(a) as sessionCount " +
           "FROM EmployeeActivity a " +
           "WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY a.appName " +
           "ORDER BY totalMs DESC")
    List<Map<String, Object>> getTopAppsForDevices(@Param("deviceIds") List<Long> deviceIds,
                                                    @Param("startTime") Long startTime,
                                                    @Param("endTime") Long endTime,
                                                    Pageable pageable);
    
    @Query("SELECT COALESCE(SUM(a.durationMs), 0) FROM EmployeeActivity a " +
           "WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime")
    Long getTotalTimeForDevices(@Param("deviceIds") List<Long> deviceIds,
                                 @Param("startTime") Long startTime,
                                 @Param("endTime") Long endTime);
    
    @Query("SELECT DATE(FROM_UNIXTIME(a.startTime/1000)) as date, " +
           "SUM(a.durationMs) as totalMs " +
           "FROM EmployeeActivity a " +
           "WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(FROM_UNIXTIME(a.startTime/1000))")
    List<Map<String, Object>> getDailyStats(@Param("deviceIds") List<Long> deviceIds,
                                             @Param("startTime") Long startTime,
                                             @Param("endTime") Long endTime);
    
    @Query("SELECT a FROM EmployeeActivity a " +
           "WHERE a.deviceId IN :deviceIds AND a.startTime >= :since " +
           "ORDER BY a.startTime DESC")
    List<EmployeeActivity> findRecentActivities(@Param("deviceIds") List<Long> deviceIds,
                                                 @Param("since") Long since,
                                                 Pageable pageable);
    
    @Query("SELECT COUNT(DISTINCT a.deviceId) FROM EmployeeActivity a " +
           "WHERE a.startTime >= :since")
    Long countActiveDevices(@Param("since") Long since);
    
    @Query("SELECT a.processName as process, COUNT(a) as count, SUM(a.durationMs) as totalMs " +
           "FROM EmployeeActivity a " +
           "WHERE a.deviceId IN :deviceIds AND a.startTime BETWEEN :startTime AND :endTime " +
           "GROUP BY a.processName " +
           "ORDER BY totalMs DESC")
    List<Map<String, Object>> getProcessBreakdown(@Param("deviceIds") List<Long> deviceIds,
                                                   @Param("startTime") Long startTime,
                                                   @Param("endTime") Long endTime);
    
    @Query(value = "SELECT a.app_name, a.window_title, a.start_time, a.end_time, a.duration_ms, " +
           "u.full_name as user_name " +
           "FROM employee_activities a " +
           "JOIN devices d ON a.device_id = d.id " +
           "JOIN users u ON d.user_id = u.id " +
           "WHERE u.organization_id = :organizationId " +
           "AND a.start_time BETWEEN :startTime AND :endTime " +
           "ORDER BY a.start_time DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> getOrganizationActivities(@Param("organizationId") Long organizationId,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime,
                                                         @Param("limit") int limit);
}