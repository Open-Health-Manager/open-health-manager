package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.FHIRPatientConsent;
import org.mitre.healthmanager.repository.FHIRPatientConsentRepository;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.mapper.FHIRPatientConsentMapper;
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

    private final FHIRPatientConsentMapper fHIRPatientConsentMapper;

    public FHIRPatientConsentService(
        FHIRPatientConsentRepository fHIRPatientConsentRepository,
        FHIRPatientConsentMapper fHIRPatientConsentMapper
    ) {
        this.fHIRPatientConsentRepository = fHIRPatientConsentRepository;
        this.fHIRPatientConsentMapper = fHIRPatientConsentMapper;
    }

    /**
     * Save a fHIRPatientConsent.
     *
     * @param fHIRPatientConsentDTO the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatientConsentDTO save(FHIRPatientConsentDTO fHIRPatientConsentDTO) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsentDTO);
        FHIRPatientConsent fHIRPatientConsent = fHIRPatientConsentMapper.toEntity(fHIRPatientConsentDTO);
        fHIRPatientConsent = fHIRPatientConsentRepository.save(fHIRPatientConsent);
        return fHIRPatientConsentMapper.toDto(fHIRPatientConsent);
    }

    /**
     * Update a fHIRPatientConsent.
     *
     * @param fHIRPatientConsentDTO the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatientConsentDTO update(FHIRPatientConsentDTO fHIRPatientConsentDTO) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsentDTO);
        FHIRPatientConsent fHIRPatientConsent = fHIRPatientConsentMapper.toEntity(fHIRPatientConsentDTO);
        fHIRPatientConsent = fHIRPatientConsentRepository.save(fHIRPatientConsent);
        return fHIRPatientConsentMapper.toDto(fHIRPatientConsent);
    }

    /**
     * Partially update a fHIRPatientConsent.
     *
     * @param fHIRPatientConsentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FHIRPatientConsentDTO> partialUpdate(FHIRPatientConsentDTO fHIRPatientConsentDTO) {
        log.debug("Request to partially update FHIRPatientConsent : {}", fHIRPatientConsentDTO);

        return fHIRPatientConsentRepository
            .findById(fHIRPatientConsentDTO.getId())
            .map(existingFHIRPatientConsent -> {
                fHIRPatientConsentMapper.partialUpdate(existingFHIRPatientConsent, fHIRPatientConsentDTO);

                return existingFHIRPatientConsent;
            })
            .map(fHIRPatientConsentRepository::save)
            .map(fHIRPatientConsentMapper::toDto);
    }

    /**
     * Get all the fHIRPatientConsents.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<FHIRPatientConsentDTO> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRPatientConsents");
        return fHIRPatientConsentRepository.findAll(pageable).map(fHIRPatientConsentMapper::toDto);
    }

    /**
     * Get all the fHIRPatientConsents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<FHIRPatientConsentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return fHIRPatientConsentRepository.findAllWithEagerRelationships(pageable).map(fHIRPatientConsentMapper::toDto);
    }

    /**
     * Get one fHIRPatientConsent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FHIRPatientConsentDTO> findOne(Long id) {
        log.debug("Request to get FHIRPatientConsent : {}", id);
        return fHIRPatientConsentRepository.findOneWithEagerRelationships(id).map(fHIRPatientConsentMapper::toDto);
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
