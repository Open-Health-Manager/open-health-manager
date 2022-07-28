package org.mitre.healthmanager.repository;

import org.mitre.healthmanager.domain.FHIRClient;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the FHIRClient entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FHIRClientRepository extends JpaRepository<FHIRClient, Long> {}
