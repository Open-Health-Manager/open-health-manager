package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.FHIRPatientConsent;
import org.mitre.healthmanager.repository.FHIRPatientConsentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FHIRPatientConsent}.
 */
@Service
@Transactional
public class FHIRPatientConsentService {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientConsentService.class);

    private final FHIRPatientConsentRepository fHIRPatientConsentRepository;

    public FHIRPatientConsentService(FHIRPatientConsentRepository fHIRPatientConsentRepository) {
        this.fHIRPatientConsentRepository = fHIRPatientConsentRepository;
    }

    /**
     * Save a fHIRPatientConsent.
     *
     * @param fHIRPatientConsent the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatientConsent save(FHIRPatientConsent fHIRPatientConsent) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsent);
        return fHIRPatientConsentRepository.save(fHIRPatientConsent);
    }

    /**
     * Update a fHIRPatientConsent.
     *
     * @param fHIRPatientConsent the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatientConsent update(FHIRPatientConsent fHIRPatientConsent) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsent);
        return fHIRPatientConsentRepository.save(fHIRPatientConsent);
    }

    /**
     * Partially update a fHIRPatientConsent.
     *
     * @param fHIRPatientConsent the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FHIRPatientConsent> partialUpdate(FHIRPatientConsent fHIRPatientConsent) {
        log.debug("Request to partially update FHIRPatientConsent : {}", fHIRPatientConsent);

        return fHIRPatientConsentRepository
            .findById(fHIRPatientConsent.getId())
            .map(existingFHIRPatientConsent -> {
                if (fHIRPatientConsent.getApprove() != null) {
                    existingFHIRPatientConsent.setApprove(fHIRPatientConsent.getApprove());
                }
                if (fHIRPatientConsent.getFhirResource() != null) {
                    existingFHIRPatientConsent.setFhirResource(fHIRPatientConsent.getFhirResource());
                }

                return existingFHIRPatientConsent;
            })
            .map(fHIRPatientConsentRepository::save);
    }

    /**
     * Get all the fHIRPatientConsents.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<FHIRPatientConsent> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRPatientConsents");
        return fHIRPatientConsentRepository.findAll(pageable);
    }

    /**
     * Get all the fHIRPatientConsents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<FHIRPatientConsent> findAllWithEagerRelationships(Pageable pageable) {
        return fHIRPatientConsentRepository.findAllWithEagerRelationships(pageable);
    }

    /**
     * Get one fHIRPatientConsent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FHIRPatientConsent> findOne(Long id) {
        log.debug("Request to get FHIRPatientConsent : {}", id);
        return fHIRPatientConsentRepository.findOneWithEagerRelationships(id);
    }

    /**
     * Delete the fHIRPatientConsent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete FHIRPatientConsent : {}", id);
        fHIRPatientConsentRepository.deleteById(id);
    }
}
