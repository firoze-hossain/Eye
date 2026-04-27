// src/main/java/com/trackeye/repository/UserRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndOrganizationId(String email, Long organizationId);

    List<User> findByOrganizationId(Long organizationId);

    List<User> findByOrganizationIdAndRole(Long organizationId, String role);

    List<User> findByOrganizationIdAndStatus(Long organizationId, String status);

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.organizationId = :orgId AND u.status = 'active'")
    long countActiveUsersByOrganizationId(@Param("orgId") Long orgId);
}