package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.mapper.FHIRClientMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link FHIRClient}.
 */
@Service
@Transactional
public class FHIRClientService {

    private final Logger log = LoggerFactory.getLogger(FHIRClientService.class);

    private final FHIRClientRepository fHIRClientRepository;

    private final FHIRClientMapper fHIRClientMapper;

    public FHIRClientService(FHIRClientRepository fHIRClientRepository, FHIRClientMapper fHIRClientMapper) {
        this.fHIRClientRepository = fHIRClientRepository;
        this.fHIRClientMapper = fHIRClientMapper;
    }

    /**
     * Save a fHIRClient.
     *
     * @param fHIRClientDTO the entity to save.
     * @return the persisted entity.
     */
    public FHIRClientDTO save(FHIRClientDTO fHIRClientDTO) {
        log.debug("Request to save FHIRClient : {}", fHIRClientDTO);
        FHIRClient fHIRClient = fHIRClientMapper.toEntity(fHIRClientDTO);
        fHIRClient = fHIRClientRepository.save(fHIRClient);
        return fHIRClientMapper.toDto(fHIRClient);
    }

    /**
     * Update a fHIRClient.
     *
     * @param fHIRClientDTO the entity to save.
     * @return the persisted entity.
     */
    public FHIRClientDTO update(FHIRClientDTO fHIRClientDTO) {
        log.debug("Request to save FHIRClient : {}", fHIRClientDTO);
        FHIRClient fHIRClient = fHIRClientMapper.toEntity(fHIRClientDTO);
        fHIRClient = fHIRClientRepository.save(fHIRClient);
        return fHIRClientMapper.toDto(fHIRClient);
    }

    /**
     * Partially update a fHIRClient.
     *
     * @param fHIRClientDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FHIRClientDTO> partialUpdate(FHIRClientDTO fHIRClientDTO) {
        log.debug("Request to partially update FHIRClient : {}", fHIRClientDTO);

        return fHIRClientRepository
            .findById(fHIRClientDTO.getId())
            .map(existingFHIRClient -> {
                fHIRClientMapper.partialUpdate(existingFHIRClient, fHIRClientDTO);

                return existingFHIRClient;
            })
            .map(fHIRClientRepository::save)
            .map(fHIRClientMapper::toDto);
    }

    /**
     * Get all the fHIRClients.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<FHIRClientDTO> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRClients");
        return fHIRClientRepository.findAll(pageable).map(fHIRClientMapper::toDto);
    }

    /**
     * Get one fHIRClient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FHIRClientDTO> findOne(Long id) {
        log.debug("Request to get FHIRClient : {}", id);
        return fHIRClientRepository.findById(id).map(fHIRClientMapper::toDto);
    }

    /**
     * Delete the fHIRClient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete FHIRClient : {}", id);
        fHIRClientRepository.deleteById(id);
    }
}
