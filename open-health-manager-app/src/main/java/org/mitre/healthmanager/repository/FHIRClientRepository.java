package org.mitre.healthmanager.repository;

import java.util.List;

import org.mitre.healthmanager.domain.FHIRClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the FHIRClient entity.
 */
@Repository
public interface FHIRClientRepository extends JpaRepository<FHIRClient, Long> {
	
    List<FHIRClient> findByFhirOrganizationId(String fhirOrganizationId);
}
