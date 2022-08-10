package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.mitre.healthmanager.repository.UserDUARepository;
import org.mitre.healthmanager.service.UserDUAService;
import org.mitre.healthmanager.service.dto.UserDUADTO;
import org.mitre.healthmanager.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.mitre.healthmanager.security.AuthoritiesConstants;

/**
 * REST controller for managing {@link org.mitre.healthmanager.domain.UserDUA}.
 */
@RestController
@RequestMapping("/api/admin")
public class UserDUAResource {

    private final Logger log = LoggerFactory.getLogger(UserDUAResource.class);

    private static final String ENTITY_NAME = "userDUA";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserDUAService userDUAService;

    private final UserDUARepository userDUARepository;

    public UserDUAResource(UserDUAService userDUAService, UserDUARepository userDUARepository) {
        this.userDUAService = userDUAService;
        this.userDUARepository = userDUARepository;
    }

    /**
     * {@code POST  /user-duas} : Create a new userDUA.
     *
     * @param userDUADTO the userDUADTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userDUADTO, or with status {@code 400 (Bad Request)} if the userDUA has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/user-duas")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<UserDUADTO> createUserDUA(@Valid @RequestBody UserDUADTO userDUADTO) throws URISyntaxException {
        log.debug("REST request to save UserDUA : {}", userDUADTO);
        if (userDUADTO.getId() != null) {
            throw new BadRequestAlertException("A new userDUA cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UserDUADTO result = userDUAService.save(userDUADTO);
        return ResponseEntity
            .created(new URI("/api/user-duas/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /user-duas/:id} : Updates an existing userDUA.
     *
     * @param id the id of the userDUADTO to save.
     * @param userDUADTO the userDUADTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userDUADTO,
     * or with status {@code 400 (Bad Request)} if the userDUADTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userDUADTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/user-duas/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<UserDUADTO> updateUserDUA(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody UserDUADTO userDUADTO
    ) throws URISyntaxException {
        log.debug("REST request to update UserDUA : {}, {}", id, userDUADTO);
        if (userDUADTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userDUADTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userDUARepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        UserDUADTO result = userDUAService.update(userDUADTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userDUADTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /user-duas/:id} : Partial updates given fields of an existing userDUA, field will ignore if it is null
     *
     * @param id the id of the userDUADTO to save.
     * @param userDUADTO the userDUADTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userDUADTO,
     * or with status {@code 400 (Bad Request)} if the userDUADTO is not valid,
     * or with status {@code 404 (Not Found)} if the userDUADTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the userDUADTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/user-duas/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<UserDUADTO> partialUpdateUserDUA(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody UserDUADTO userDUADTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update UserDUA partially : {}, {}", id, userDUADTO);
        if (userDUADTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userDUADTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userDUARepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserDUADTO> result = userDUAService.partialUpdate(userDUADTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userDUADTO.getId().toString())
        );
    }

    /**
     * {@code GET  /user-duas} : get all the userDUAS.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userDUAS in body.
     */
    @GetMapping("/user-duas")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<UserDUADTO>> getAllUserDUAS(
        @org.springdoc.api.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false, defaultValue = "true") boolean eagerload
    ) {
        log.debug("REST request to get a page of UserDUAS");
        Page<UserDUADTO> page;
        if (eagerload) {
            page = userDUAService.findAllWithEagerRelationships(pageable);
        } else {
            page = userDUAService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /user-duas/:id} : get the "id" userDUA.
     *
     * @param id the id of the userDUADTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userDUADTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/user-duas/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<UserDUADTO> getUserDUA(@PathVariable Long id) {
        log.debug("REST request to get UserDUA : {}", id);
        Optional<UserDUADTO> userDUADTO = userDUAService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userDUADTO);
    }

    /**
     * {@code DELETE  /user-duas/:id} : delete the "id" userDUA.
     *
     * @param id the id of the userDUADTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/user-duas/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteUserDUA(@PathVariable Long id) {
        log.debug("REST request to delete UserDUA : {}", id);
        userDUAService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
