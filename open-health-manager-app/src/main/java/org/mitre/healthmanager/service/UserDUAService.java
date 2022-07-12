package org.mitre.healthmanager.service;

import java.util.Optional;
import org.mitre.healthmanager.domain.UserDUA;
import org.mitre.healthmanager.repository.UserDUARepository;
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

    public UserDUAService(UserDUARepository userDUARepository) {
        this.userDUARepository = userDUARepository;
    }

    /**
     * Save a userDUA.
     *
     * @param userDUA the entity to save.
     * @return the persisted entity.
     */
    public UserDUA save(UserDUA userDUA) {
        if (userDIA.getActiveDate() == null) {
            userDUA.setActiveDate(Instant.now());
        }
        log.debug("Request to save UserDUA : {}", userDUA);
        return userDUARepository.save(userDUA);
    }

    /**
     * Update a userDUA.
     *
     * @param userDUA the entity to save.
     * @return the persisted entity.
     */
    public UserDUA update(UserDUA userDUA) {
        log.debug("Request to save UserDUA : {}", userDUA);
        return userDUARepository.save(userDUA);
    }

    /**
     * Partially update a userDUA.
     *
     * @param userDUA the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserDUA> partialUpdate(UserDUA userDUA) {
        log.debug("Request to partially update UserDUA : {}", userDUA);

        return userDUARepository
            .findById(userDUA.getId())
            .map(existingUserDUA -> {
                if (userDUA.getActive() != null) {
                    existingUserDUA.setActive(userDUA.getActive());
                }
                if (userDUA.getVersion() != null) {
                    existingUserDUA.setVersion(userDUA.getVersion());
                }
                if (userDUA.getAgeAttested() != null) {
                    existingUserDUA.setAgeAttested(userDUA.getAgeAttested());
                }
                if (userDUA.getActiveDate() != null) {
                    existingUserDUA.setActiveDate(userDUA.getActiveDate());
                }
                if (userDUA.getRevocationDate() != null) {
                    existingUserDUA.setRevocationDate(userDUA.getRevocationDate());
                }

                return existingUserDUA;
            })
            .map(userDUARepository::save);
    }

    /**
     * Get all the userDUAS.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<UserDUA> findAll(Pageable pageable) {
        log.debug("Request to get all UserDUAS");
        return userDUARepository.findAll(pageable);
    }

    /**
     * Get all the userDUAS with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<UserDUA> findAllWithEagerRelationships(Pageable pageable) {
        return userDUARepository.findAllWithEagerRelationships(pageable);
    }

    /**
     * Get one userDUA by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserDUA> findOne(Long id) {
        log.debug("Request to get UserDUA : {}", id);
        return userDUARepository.findOneWithEagerRelationships(id);
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
