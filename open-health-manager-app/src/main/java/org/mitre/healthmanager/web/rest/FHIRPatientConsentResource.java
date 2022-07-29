package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import org.mitre.healthmanager.domain.FHIRPatientConsent;
import org.mitre.healthmanager.repository.FHIRPatientConsentRepository;
import org.mitre.healthmanager.service.FHIRPatientConsentService;
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

/**
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRPatientConsent}.
 */
@RestController
@RequestMapping("/api")
public class FHIRPatientConsentResource {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientConsentResource.class);

    private final FHIRPatientConsentService fHIRPatientConsentService;

    private final FHIRPatientConsentRepository fHIRPatientConsentRepository;

    public FHIRPatientConsentResource(
        FHIRPatientConsentService fHIRPatientConsentService,
        FHIRPatientConsentRepository fHIRPatientConsentRepository
    ) {
        this.fHIRPatientConsentService = fHIRPatientConsentService;
        this.fHIRPatientConsentRepository = fHIRPatientConsentRepository;
    }

    /**
     * {@code GET  /fhir-patient-consents} : get all the fHIRPatientConsents.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRPatientConsents in body.
     */
    @GetMapping("/fhir-patient-consents")
    public ResponseEntity<List<FHIRPatientConsent>> getAllFHIRPatientConsents(
        @org.springdoc.api.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false, defaultValue = "true") boolean eagerload
    ) {
        log.debug("REST request to get a page of FHIRPatientConsents");
        Page<FHIRPatientConsent> page;
        if (eagerload) {
            page = fHIRPatientConsentService.findAllWithEagerRelationships(pageable);
        } else {
            page = fHIRPatientConsentService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /fhir-patient-consents/:id} : get the "id" fHIRPatientConsent.
     *
     * @param id the id of the fHIRPatientConsent to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fHIRPatientConsent, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/fhir-patient-consents/{id}")
    public ResponseEntity<FHIRPatientConsent> getFHIRPatientConsent(@PathVariable Long id) {
        log.debug("REST request to get FHIRPatientConsent : {}", id);
        Optional<FHIRPatientConsent> fHIRPatientConsent = fHIRPatientConsentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(fHIRPatientConsent);
    }
}
