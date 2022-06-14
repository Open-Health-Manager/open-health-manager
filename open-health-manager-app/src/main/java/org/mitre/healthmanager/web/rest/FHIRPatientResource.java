package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRPatient}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class FHIRPatientResource {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientResource.class);

    private static final String ENTITY_NAME = "fHIRPatient";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FHIRPatientRepository fHIRPatientRepository;

    public FHIRPatientResource(FHIRPatientRepository fHIRPatientRepository) {
        this.fHIRPatientRepository = fHIRPatientRepository;
    }

    /**
     * {@code POST  /fhir-patients} : Create a new fHIRPatient.
     *
     * @param fHIRPatient the fHIRPatient to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new fHIRPatient, or with status {@code 400 (Bad Request)} if the fHIRPatient has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/fhir-patients")
    public ResponseEntity<FHIRPatient> createFHIRPatient(@Valid @RequestBody FHIRPatient fHIRPatient) throws URISyntaxException {
        log.debug("REST request to save FHIRPatient : {}", fHIRPatient);
        if (fHIRPatient.getId() != null) {
            throw new BadRequestAlertException("A new fHIRPatient cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FHIRPatient result = fHIRPatientRepository.save(fHIRPatient);
        return ResponseEntity
            .created(new URI("/api/fhir-patients/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /fhir-patients/:id} : Updates an existing fHIRPatient.
     *
     * @param id the id of the fHIRPatient to save.
     * @param fHIRPatient the fHIRPatient to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fHIRPatient,
     * or with status {@code 400 (Bad Request)} if the fHIRPatient is not valid,
     * or with status {@code 500 (Internal Server Error)} if the fHIRPatient couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/fhir-patients/{id}")
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

        FHIRPatient result = fHIRPatientRepository.save(fHIRPatient);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRPatient.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /fhir-patients/:id} : Partial updates given fields of an existing fHIRPatient, field will ignore if it is null
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

        Optional<FHIRPatient> result = fHIRPatientRepository
            .findById(fHIRPatient.getId())
            .map(existingFHIRPatient -> {
                if (fHIRPatient.getFhirId() != null) {
                    existingFHIRPatient.setFhirId(fHIRPatient.getFhirId());
                }

                return existingFHIRPatient;
            })
            .map(fHIRPatientRepository::save);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRPatient.getId().toString())
        );
    }

    /**
     * {@code GET  /fhir-patients} : get all the fHIRPatients.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRPatients in body.
     */
    @GetMapping("/fhir-patients")
    public List<FHIRPatient> getAllFHIRPatients(@RequestParam(required = false, defaultValue = "false") boolean eagerload) {
       
        log.debug("REST request to get all FHIRPatients");
        return fHIRPatientRepository.findAllWithEagerRelationships();
       
    }

    /**
     * {@code GET  /fhir-patients/:id} : get the "id" fHIRPatient.
     *
     * @param id the id of the fHIRPatient to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fHIRPatient, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/fhir-patients/{id}")
    public ResponseEntity<FHIRPatient> getFHIRPatient(@PathVariable Long id) {
        log.debug("REST request to get FHIRPatient : {}", id);
        Optional<FHIRPatient> fHIRPatient = fHIRPatientRepository.findOneWithEagerRelationships(id);
        return ResponseUtil.wrapOrNotFound(fHIRPatient);
    }

    /**
     * {@code DELETE  /fhir-patients/:id} : delete the "id" fHIRPatient.
     *
     * @param id the id of the fHIRPatient to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/fhir-patients/{id}")
    public ResponseEntity<Void> deleteFHIRPatient(@PathVariable Long id) {
        log.debug("REST request to delete FHIRPatient : {}", id);
        fHIRPatientRepository.deleteById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
