// src/main/java/com/trackeye/repository/BrowserActivityRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.EmployeeBrowserActivity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface BrowserActivityRepository extends JpaRepository<EmployeeBrowserActivity, Long> {

    List<EmployeeBrowserActivity> findByDeviceIdOrderByStartTimeDesc(Long deviceId);

    List<EmployeeBrowserActivity> findByDeviceIdAndStartTimeBetween(Long deviceId, Long startTime, Long endTime);

    @Query("SELECT b FROM EmployeeBrowserActivity b WHERE b.deviceId IN :deviceIds AND b.startTime BETWEEN :startTime AND :endTime ORDER BY b.startTime DESC")
    List<EmployeeBrowserActivity> findByDeviceIdsAndTimeRange(@Param("deviceIds") List<Long> deviceIds,
                                                              @Param("startTime") Long startTime,
                                                              @Param("endTime") Long endTime);

    @Query(value = "SELECT b.url, COUNT(b.id) as visit_count, SUM(b.duration_ms) as total_ms " +
            "FROM employee_browser_activities b " +
            "WHERE b.device_id IN (:deviceIds) " +
            "AND b.start_time BETWEEN :startTime AND :endTime " +
            "AND b.url IS NOT NULL AND b.url != '' " +
            "GROUP BY b.url " +
            "ORDER BY total_ms DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> getTopWebsitesForDevices(@Param("deviceIds") List<Long> deviceIds,
                                                       @Param("startTime") Long startTime,
                                                       @Param("endTime") Long endTime,
                                                       @Param("limit") int limit);

    @Query(value = "SELECT b.browser_name as browser, COUNT(b.id) as session_count, SUM(b.duration_ms) as total_ms " +
            "FROM employee_browser_activities b " +
            "WHERE b.device_id IN (:deviceIds) " +
            "AND b.start_time BETWEEN :startTime AND :endTime " +
            "GROUP BY b.browser_name " +
            "ORDER BY total_ms DESC", nativeQuery = true)
    List<Map<String, Object>> getBrowserUsageStats(@Param("deviceIds") List<Long> deviceIds,
                                                   @Param("startTime") Long startTime,
                                                   @Param("endTime") Long endTime);

    @Query(value = "SELECT " +
            "SUBSTRING(b.url, 1, POSITION('/' IN SUBSTRING(b.url FROM 9))) as domain, " +
            "SUM(b.duration_ms) as total_ms " +
            "FROM employee_browser_activities b " +
            "WHERE b.device_id IN (:deviceIds) " +
            "AND b.start_time BETWEEN :startTime AND :endTime " +
            "AND b.url IS NOT NULL AND b.url != '' " +
            "GROUP BY domain " +
            "ORDER BY total_ms DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> getTopDomainsForDevices(@Param("deviceIds") List<Long> deviceIds,
                                                      @Param("startTime") Long startTime,
                                                      @Param("endTime") Long endTime,
                                                      @Param("limit") int limit);

    @Query(value = "SELECT b.url, b.page_title, b.start_time, b.duration_ms, " +
            "u.full_name as user_name, d.device_name " +
            "FROM employee_browser_activities b " +
            "JOIN devices d ON b.device_id = d.id " +
            "JOIN users u ON d.user_id = u.id " +
            "WHERE u.organization_id = :organizationId " +
            "AND b.start_time BETWEEN :startTime AND :endTime " +
            "ORDER BY b.start_time DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Map<String, Object>> getOrganizationBrowserActivities(@Param("organizationId") Long organizationId,
                                                               @Param("startTime") Long startTime,
                                                               @Param("endTime") Long endTime,
                                                               @Param("limit") int limit);

    @Query("SELECT COUNT(DISTINCT b.deviceId) FROM EmployeeBrowserActivity b WHERE b.startTime >= :since")
    Long countActiveBrowserDevices(@Param("since") Long since);
}