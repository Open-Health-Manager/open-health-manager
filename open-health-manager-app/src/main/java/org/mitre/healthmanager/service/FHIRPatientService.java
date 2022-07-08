package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FHIRPatient}.
 */
@Service
@Transactional("jhipsterTransactionManager")
public class FHIRPatientService {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientService.class);

    private final FHIRPatientRepository fHIRPatientRepository;

    public FHIRPatientService(FHIRPatientRepository fHIRPatientRepository) {
        this.fHIRPatientRepository = fHIRPatientRepository;
    }

    /**
     * Save a fHIRPatient.
     *
     * @param fHIRPatient the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatient save(FHIRPatient fHIRPatient) {
        log.debug("Request to save FHIRPatient : {}", fHIRPatient);
        return fHIRPatientRepository.save(fHIRPatient);
    }

    /**
     * Update a fHIRPatient.
     *
     * @param fHIRPatient the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatient update(FHIRPatient fHIRPatient) {
        log.debug("Request to save FHIRPatient : {}", fHIRPatient);
        return fHIRPatientRepository.save(fHIRPatient);
    }

    /**
     * Partially update a fHIRPatient.
     *
     * @param fHIRPatient the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FHIRPatient> partialUpdate(FHIRPatient fHIRPatient) {
        log.debug("Request to partially update FHIRPatient : {}", fHIRPatient);

        return fHIRPatientRepository
            .findById(fHIRPatient.getId())
            .map(existingFHIRPatient -> {
                if (fHIRPatient.getFhirId() != null) {
                    existingFHIRPatient.setFhirId(fHIRPatient.getFhirId());
                }

                return existingFHIRPatient;
            })
            .map(fHIRPatientRepository::save);
    }

    /**
     * Get all the fHIRPatients.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(transactionManager = "jhipsterTransactionManager", readOnly = true)
    public Page<FHIRPatient> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRPatients");
        return fHIRPatientRepository.findAll(pageable);
    }

    /**
     * Get all the fHIRPatients with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<FHIRPatient> findAllWithEagerRelationships(Pageable pageable) {
        return fHIRPatientRepository.findAllWithEagerRelationships(pageable);
    }

    /**
     * Get one fHIRPatient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(transactionManager = "jhipsterTransactionManager", readOnly = true)
    public Optional<FHIRPatient> findOne(Long id) {
        log.debug("Request to get FHIRPatient : {}", id);
        return fHIRPatientRepository.findOneWithEagerRelationships(id);
    }

    /**
     * Delete the fHIRPatient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete FHIRPatient : {}", id);
        fHIRPatientRepository.deleteById(id);
    }
}
