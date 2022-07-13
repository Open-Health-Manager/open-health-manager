package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.UserDUA;
import org.mitre.healthmanager.repository.UserDUARepository;
import org.mitre.healthmanager.service.dto.UserDUADTO;
import org.mitre.healthmanager.service.mapper.UserDUAMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

/**
 * Service Implementation for managing {@link UserDUA}.
 */
@Service
@Transactional("jhipsterTransactionManager")
public class UserDUAService {

    private final Logger log = LoggerFactory.getLogger(UserDUAService.class);

    private final UserDUARepository userDUARepository;

    private final UserDUAMapper userDUAMapper;

    public UserDUAService(UserDUARepository userDUARepository, UserDUAMapper userDUAMapper) {
        this.userDUARepository = userDUARepository;
        this.userDUAMapper = userDUAMapper;
    }

    /**
     * Save a userDUA.
     *
     * @param userDUADTO the entity to save.
     * @return the persisted entity.
     */
    public UserDUADTO save(UserDUADTO userDUADTO) {
        log.debug("Request to save UserDUA : {}", userDUADTO);
        UserDUA userDUA = userDUAMapper.toEntity(userDUADTO);
        // set active date to now if null
        if (userDUA.getActiveDate() == null) {
            userDUA.setActiveDate(Instant.now());
        }
        userDUA = userDUARepository.save(userDUA);
        return userDUAMapper.toDto(userDUA);
    }

    /**
     * Update a userDUA.
     *
     * @param userDUADTO the entity to save.
     * @return the persisted entity.
     */
    public UserDUADTO update(UserDUADTO userDUADTO) {
        log.debug("Request to save UserDUA : {}", userDUADTO);
        UserDUA userDUA = userDUAMapper.toEntity(userDUADTO);
        userDUA = userDUARepository.save(userDUA);
        return userDUAMapper.toDto(userDUA);
    }

    /**
     * Partially update a userDUA.
     *
     * @param userDUADTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserDUADTO> partialUpdate(UserDUADTO userDUADTO) {
        log.debug("Request to partially update UserDUA : {}", userDUADTO);

        return userDUARepository
            .findById(userDUADTO.getId())
            .map(existingUserDUA -> {
                userDUAMapper.partialUpdate(existingUserDUA, userDUADTO);

                return existingUserDUA;
            })
            .map(userDUARepository::save)
            .map(userDUAMapper::toDto);
    }

    /**
     * Get all the userDUAS.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<UserDUADTO> findAll(Pageable pageable) {
        log.debug("Request to get all UserDUAS");
        return userDUARepository.findAll(pageable).map(userDUAMapper::toDto);
    }

    /**
     * Get all the userDUAS with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<UserDUADTO> findAllWithEagerRelationships(Pageable pageable) {
        return userDUARepository.findAllWithEagerRelationships(pageable).map(userDUAMapper::toDto);
    }

    /**
     * Get one userDUA by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserDUADTO> findOne(Long id) {
        log.debug("Request to get UserDUA : {}", id);
        return userDUARepository.findOneWithEagerRelationships(id).map(userDUAMapper::toDto);
    }

    /**
     * Delete the userDUA by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete UserDUA : {}", id);
        userDUARepository.deleteById(id);
    }
}
