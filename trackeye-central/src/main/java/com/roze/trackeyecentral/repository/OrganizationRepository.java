// src/main/java/com/trackeye/repository/OrganizationRepository.java
package com.roze.trackeyecentral.repository;

import com.roze.trackeyecentral.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySubdomain(String subdomain);

    Optional<Organization> findByOrgName(String orgName);

    boolean existsBySubdomain(String subdomain);
}