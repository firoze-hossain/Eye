package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.PolicyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRuleRepository extends JpaRepository<PolicyRule, Long> {
    List<PolicyRule> findByOrganizationId(Long organizationId);

    List<PolicyRule> findByOrganizationIdAndActiveTrue(Long organizationId);
}
