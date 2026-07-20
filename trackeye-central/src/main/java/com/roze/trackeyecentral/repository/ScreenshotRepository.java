// src/main/java/com/trackeye/repository/ScreenshotRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.EmployeeScreenshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ScreenshotRepository extends JpaRepository<EmployeeScreenshot, Long> {
    
    List<EmployeeScreenshot> findByDeviceIdOrderByTimestampDesc(Long deviceId);
    
    List<EmployeeScreenshot> findByDeviceIdAndTimestampBetweenOrderByTimestampDesc(Long deviceId, Long startTime, Long endTime);
    
    @Query("SELECT s FROM EmployeeScreenshot s WHERE s.deviceId IN :deviceIds AND s.timestamp BETWEEN :startTime AND :endTime ORDER BY s.timestamp DESC")
    List<EmployeeScreenshot> findByDeviceIdsAndTimeRange(@Param("deviceIds") List<Long> deviceIds,
                                                          @Param("startTime") Long startTime,
                                                          @Param("endTime") Long endTime);
    
    @Query("SELECT s FROM EmployeeScreenshot s WHERE s.deviceId IN :deviceIds AND s.timestamp >= :since ORDER BY s.timestamp DESC")
    List<EmployeeScreenshot> findRecentScreenshots(@Param("deviceIds") List<Long> deviceIds,
                                                    @Param("since") Long since,
                                                    Pageable pageable);
    
    @Query("SELECT COUNT(s) FROM EmployeeScreenshot s WHERE s.deviceId = :deviceId AND s.timestamp BETWEEN :startTime AND :endTime")
    Long countScreenshotsForDevice(@Param("deviceId") Long deviceId,
                                    @Param("startTime") Long startTime,
                                    @Param("endTime") Long endTime);
    
    @Query(value = "SELECT s.* FROM employee_screenshots s " +
           "JOIN devices d ON s.device_id = d.id " +
           "JOIN users u ON d.user_id = u.id " +
           "WHERE u.organization_id = :organizationId " +
           "AND s.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY s.timestamp DESC", nativeQuery = true)
    List<EmployeeScreenshot> getOrganizationScreenshots(@Param("organizationId") Long organizationId,
                                                         @Param("startTime") Long startTime,
                                                         @Param("endTime") Long endTime,
                                                         Pageable pageable);
    
    @Modifying
    @Query("DELETE FROM EmployeeScreenshot s WHERE s.timestamp < :olderThan")
    int deleteOldScreenshots(@Param("olderThan") Long olderThan);
    
    @Query("SELECT COUNT(DISTINCT s.deviceId) FROM EmployeeScreenshot s WHERE s.timestamp >= :since")
    Long countActiveScreenshotDevices(@Param("since") Long since);
    
    @Query("SELECT s.screenshotHash, COUNT(s) as count FROM EmployeeScreenshot s " +
           "WHERE s.deviceId IN :deviceIds GROUP BY s.screenshotHash HAVING COUNT(s) > 1")
    List<Object[]> findDuplicateScreenshots(@Param("deviceIds") List<Long> deviceIds);
}