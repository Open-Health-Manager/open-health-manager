package org.mitre.healthmanager.repository;

import java.util.List;
import java.util.Optional;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the FHIRPatient entity.
 */
@Repository
public interface FHIRPatientRepository extends JpaRepository<FHIRPatient, Long> {
    default Optional<FHIRPatient> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<FHIRPatient> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<FHIRPatient> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select distinct fHIRPatient from FHIRPatient fHIRPatient left join fetch fHIRPatient.user",
        countQuery = "select count(distinct fHIRPatient) from FHIRPatient fHIRPatient"
    )
    Page<FHIRPatient> findAllWithToOneRelationships(Pageable pageable);

    @Query("select distinct fHIRPatient from FHIRPatient fHIRPatient left join fetch fHIRPatient.user")
    List<FHIRPatient> findAllWithToOneRelationships();

    @Query("select fHIRPatient from FHIRPatient fHIRPatient left join fetch fHIRPatient.user where fHIRPatient.id =:id")
    Optional<FHIRPatient> findOneWithToOneRelationships(@Param("id") Long id);
}
