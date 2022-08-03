package org.mitre.healthmanager.repository;

import java.util.List;

import org.mitre.healthmanager.domain.FHIRClient;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Spring Data SQL repository for the FHIRClient entity.
 */
@Repository
public interface FHIRClientRepository extends JpaRepository<FHIRClient, Long> {
	
    List<FHIRClient> findByFhirOrganizationId(String fhirOrganizationId);
    
    @Query("select fHIRClient from FHIRClient fHIRClient where fhirOrganizationId =:id")
    List<FHIRClient> findAllForFhirOrganizationID(@Param("id") String id);
}
