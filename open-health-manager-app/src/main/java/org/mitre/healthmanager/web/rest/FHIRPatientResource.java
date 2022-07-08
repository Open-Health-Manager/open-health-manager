package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRPatientService;
import org.mitre.healthmanager.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRPatient}.
 */
@RestController
@RequestMapping("/api/admin")
public class FHIRPatientResource {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientResource.class);

    private static final String ENTITY_NAME = "fHIRPatient";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FHIRPatientService fHIRPatientService;

    private final FHIRPatientRepository fHIRPatientRepository;

    public FHIRPatientResource(FHIRPatientService fHIRPatientService, FHIRPatientRepository fHIRPatientRepository) {
        this.fHIRPatientService = fHIRPatientService;
        this.fHIRPatientRepository = fHIRPatientRepository;
    }

    /**
     * {@code POST  /admin/fhir-patients} : Create a new fHIRPatient.
     *
     * @param fHIRPatient the fHIRPatient to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new fHIRPatient, or with status {@code 400 (Bad Request)} if the fHIRPatient has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/fhir-patients")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRPatient> createFHIRPatient(@Valid @RequestBody FHIRPatient fHIRPatient) throws URISyntaxException {
        log.debug("REST request to save FHIRPatient : {}", fHIRPatient);
        if (fHIRPatient.getId() != null) {
            throw new BadRequestAlertException("A new fHIRPatient cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FHIRPatient result = fHIRPatientService.save(fHIRPatient);
        return ResponseEntity
            .created(new URI("/api/fhir-patients/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /admin/fhir-patients/:id} : Updates an existing fHIRPatient.
     *
     * @param id the id of the fHIRPatient to save.
     * @param fHIRPatient the fHIRPatient to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fHIRPatient,
     * or with status {@code 400 (Bad Request)} if the fHIRPatient is not valid,
     * or with status {@code 500 (Internal Server Error)} if the fHIRPatient couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/fhir-patients/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRPatient> updateFHIRPatient(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody FHIRPatient fHIRPatient
    ) throws URISyntaxException {
        log.debug("REST request to update FHIRPatient : {}, {}", id, fHIRPatient);
        if (fHIRPatient.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fHIRPatient.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!fHIRPatientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        FHIRPatient result = fHIRPatientService.update(fHIRPatient);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRPatient.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /admin/fhir-patients/:id} : Partial updates given fields of an existing fHIRPatient, field will ignore if it is null
     *
     * @param id the id of the fHIRPatient to save.
     * @param fHIRPatient the fHIRPatient to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fHIRPatient,
     * or with status {@code 400 (Bad Request)} if the fHIRPatient is not valid,
     * or with status {@code 404 (Not Found)} if the fHIRPatient is not found,
     * or with status {@code 500 (Internal Server Error)} if the fHIRPatient couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/fhir-patients/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRPatient> partialUpdateFHIRPatient(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody FHIRPatient fHIRPatient
    ) throws URISyntaxException {
        log.debug("REST request to partial update FHIRPatient partially : {}, {}", id, fHIRPatient);
        if (fHIRPatient.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fHIRPatient.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!fHIRPatientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<FHIRPatient> result = fHIRPatientService.partialUpdate(fHIRPatient);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRPatient.getId().toString())
        );
    }

    /**
     * {@code GET  /admin/fhir-patients} : get all the fHIRPatients.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRPatients in body.
     */
    @GetMapping("/fhir-patients")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<FHIRPatient>> getAllFHIRPatients(
        @org.springdoc.api.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false, defaultValue = "true") boolean eagerload
    ) {
        log.debug("REST request to get a page of FHIRPatients");
        Page<FHIRPatient> page;
        if (eagerload) {
            page = fHIRPatientService.findAllWithEagerRelationships(pageable);
        } else {
            page = fHIRPatientService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /admin/fhir-patients/:id} : get the "id" fHIRPatient.
     *
     * @param id the id of the fHIRPatient to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fHIRPatient, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/fhir-patients/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRPatient> getFHIRPatient(@PathVariable Long id) {
        log.debug("REST request to get FHIRPatient : {}", id);
        Optional<FHIRPatient> fHIRPatient = fHIRPatientService.findOne(id);
        return ResponseUtil.wrapOrNotFound(fHIRPatient);
    }

    /**
     * {@code DELETE  /admin/fhir-patients/:id} : delete the "id" fHIRPatient.
     *
     * @param id the id of the fHIRPatient to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/fhir-patients/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteFHIRPatient(@PathVariable Long id) {
        log.debug("REST request to delete FHIRPatient : {}", id);
        fHIRPatientService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
