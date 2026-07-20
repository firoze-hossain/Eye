package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.PolicyViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyViolationRepository extends JpaRepository<PolicyViolation, Long> {

    @Query("SELECT v FROM PolicyViolation v WHERE v.organizationId = :orgId AND v.occurredAt >= :since ORDER BY v.occurredAt DESC")
    List<PolicyViolation> findRecentByOrg(@Param("orgId") Long orgId, @Param("since") long since);

    @Query("SELECT v FROM PolicyViolation v WHERE v.userId = :userId AND v.occurredAt >= :since ORDER BY v.occurredAt DESC")
    List<PolicyViolation> findRecentByUser(@Param("userId") Long userId, @Param("since") long since);
}
