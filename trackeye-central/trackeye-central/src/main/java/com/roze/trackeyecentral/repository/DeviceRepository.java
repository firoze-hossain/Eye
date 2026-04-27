package com.roze.trackeyecentral.repository;// src/main/java/com/trackeye/repository/DeviceRepository.java (Enhanced)


import com.roze.trackeyecentral.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceIdentifier(String deviceIdentifier);

    Optional<Device> findByApiKey(String apiKey);

    List<Device> findByUserId(Long userId);

    List<Device> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT d FROM Device d WHERE d.userId IN (SELECT u.id FROM User u WHERE u.organizationId = :organizationId)")
    List<Device> findByOrganizationId(@Param("organizationId") Long organizationId);

    @Modifying
    @Query("UPDATE Device d SET d.lastSeenAt = :timestamp WHERE d.id = :deviceId")
    void updateLastSeen(@Param("deviceId") Long deviceId, @Param("timestamp") Long timestamp);

    @Modifying
    @Query("UPDATE Device d SET d.isActive = false WHERE d.id = :deviceId")
    void deactivateDevice(@Param("deviceId") Long deviceId);

    @Modifying
    @Query("UPDATE Device d SET d.isActive = true, d.apiKey = :apiKey, d.apiKeyCreatedAt = :createdAt WHERE d.id = :deviceId")
    void reactivateDevice(@Param("deviceId") Long deviceId,
                          @Param("apiKey") String apiKey,
                          @Param("createdAt") Long createdAt);

    @Query("SELECT COUNT(d) FROM Device d WHERE d.userId = :userId AND d.isActive = true")
    Long countActiveDevicesByUser(@Param("userId") Long userId);

    @Query("SELECT d FROM Device d WHERE d.lastSeenAt >= :since AND d.isActive = true")
    List<Device> findOnlineDevices(@Param("since") Long since);

    @Query("SELECT d.userId, COUNT(d) FROM Device d WHERE d.isActive = true GROUP BY d.userId")
    List<Object[]> getDeviceCountPerUser();

    @Modifying
    @Query("UPDATE Device d SET d.apiKey = NULL, d.apiKeyCreatedAt = NULL WHERE d.id = :deviceId")
    void clearApiKey(@Param("deviceId") Long deviceId);
}