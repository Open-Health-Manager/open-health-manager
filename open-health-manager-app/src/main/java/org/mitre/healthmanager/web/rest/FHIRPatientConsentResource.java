package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.validation.Valid;

import org.mitre.healthmanager.domain.Authority;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRPatientConsentService;
import org.mitre.healthmanager.service.UserService;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
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
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRPatientConsent}.
 */
@RestController
@RequestMapping("/api")
public class FHIRPatientConsentResource {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientConsentResource.class);
    
    private static final String ENTITY_NAME = "fhirPatientConsent";
    
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FHIRPatientConsentService fHIRPatientConsentService;
    
    private final UserService userService;

    public FHIRPatientConsentResource(
        FHIRPatientConsentService fHIRPatientConsentService,
        UserService userService
    ) {
        this.fHIRPatientConsentService = fHIRPatientConsentService;
        this.userService = userService;
    }

    /**
     * {@code GET  /fhir-patient-consents} : get all the fHIRPatientConsents.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRPatientConsents in body.
     */
    @GetMapping(value="/fhir-patient-consents", params={"page"})
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<FHIRPatientConsentDTO>> getAllFHIRPatientConsents(
        @org.springdoc.api.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false, defaultValue = "true") boolean eagerload
    ) {
        log.debug("REST request to get a page of FHIRPatientConsents");
        Page<FHIRPatientConsentDTO> page;
        if (eagerload) {
            page = fHIRPatientConsentService.findAllWithEagerRelationships(pageable);
        } else {
            page = fHIRPatientConsentService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    
    /**
     * {@code GET  /fhir-patient-consents} : get active fHIRPatientConsents for current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRPatientConsents in body.
     */
    @GetMapping("/fhir-patient-consents")
    public ResponseEntity<List<FHIRPatientConsentDTO>> getActiveUserFHIRPatientConsents() {
        log.debug("REST request to get user FHIRPatientConsents");
        User user = userService.getUserWithAuthorities().get();
        Optional<List<FHIRPatientConsentDTO>> list;
        if(isAdmin(user)) {
            list = Optional.ofNullable(fHIRPatientConsentService.findAll());
        } else {
        	list = fHIRPatientConsentService.findActiveByUser(new UserDTO(user));
        }
        return ResponseUtil.wrapOrNotFound(list);
    }

    /**
     * {@code GET  /fhir-patient-consents/:id} : get the "id" fHIRPatientConsent.
     *
     * @param id the id of the fHIRPatientConsent to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fHIRPatientConsent, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/fhir-patient-consents/{id}")
    public ResponseEntity<FHIRPatientConsentDTO> getFHIRPatientConsent(@PathVariable String id) {
        log.debug("REST request to get FHIRPatientConsent : {}", id);
        Optional<FHIRPatientConsentDTO> fHIRPatientConsent = fHIRPatientConsentService.findOne(id);
        fHIRPatientConsent.ifPresent(dto -> {
        	checkUserAuthority(dto);
        });
        return ResponseUtil.wrapOrNotFound(fHIRPatientConsent);
    }
    
    /**
     * {@code POST  /fhir-patient-consents} : Create a new FhirPatientConsent.
     *
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new FhirPatientConsentDTO, or with status {@code 400 (Bad Request)}.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/fhir-patient-consents")
    public ResponseEntity<FHIRPatientConsentDTO> createFhirPatientConsentForUser(@Valid @RequestBody FHIRPatientConsentDTO fhirPatientConsentDTO) throws URISyntaxException {
        log.debug("REST to create FhirPatientConsent for user");
        if (fhirPatientConsentDTO.getId() != null) {
            throw new BadRequestAlertException("A new fhirPatientConsentDTO cannot already have an ID", ENTITY_NAME, "idexists");
        }
        checkUserAuthority(fhirPatientConsentDTO);       
        FHIRPatientConsentDTO result = fHIRPatientConsentService.save(fhirPatientConsentDTO);
        return ResponseEntity
            .created(new URI("/api/fhir-patient-consents/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }
    
    /**
     * {@code PUT  /fhir-patient-consents} : Update a new FhirPatientConsent.
     *
     * @param id the id of the FhirPatientConsent to save.
     * @param fhirPatientConsentDTO the FHIRPatientConsentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fhirPatientConsentDTO,
     * or with status {@code 400 (Bad Request)} if the fhirPatientConsentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the fhirPatientConsentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/fhir-patient-consents/{id}")
    public ResponseEntity<FHIRPatientConsentDTO> updateFhirPatientConsentForUser(
            @PathVariable(value = "id", required = false) final Long id,
            @Valid @RequestBody FHIRPatientConsentDTO fhirPatientConsentDTO
        ) throws URISyntaxException {
        log.debug("REST request to update fhirPatientConsentDTO : {}, {}", id, fhirPatientConsentDTO);
        if (fhirPatientConsentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(String.valueOf(id), fhirPatientConsentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
                
        FHIRPatientConsentDTO original = fHIRPatientConsentService.findOne(String.valueOf(id)).orElse(null);
        if (Objects.isNull(original)) {
        	throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        checkUserAuthority(original);

        FHIRPatientConsentDTO result = fHIRPatientConsentService.update(fhirPatientConsentDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fhirPatientConsentDTO.getId().toString()))
            .body(result);
    }
    
    /**
     * {@code DELETE  /fhir-patient-consents/:id} : delete the "id" fHIRPatientConsent.
     *
     * @param id the id of the fHIRPatientConsent resource to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/fhir-patient-consents/{id}")
    public ResponseEntity<Void> deleteFHIRPatientConsent(@PathVariable Long id) {
        log.debug("REST request to delete FHIRPatientConsent : {}", id);
        FHIRPatientConsentDTO original = fHIRPatientConsentService.findOne(String.valueOf(id)).orElse(null);
        if (!Objects.isNull(original)) {
        	checkUserAuthority(original);
        }        
        
        fHIRPatientConsentService.delete(String.valueOf(id));
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
    
    private void checkUserAuthority(FHIRPatientConsentDTO fhirPatientConsentDTO) {
    	User user = userService.getUserWithAuthorities().get();
    	if (!isAdmin(user) && user.getId() != fhirPatientConsentDTO.getUser().getId()) {
        	throw new BadRequestAlertException("Consent can only be updated for the current user", ENTITY_NAME, "wronguser");        	      
    	}
    }
    
    private boolean isAdmin(User user) {
    	return user.getAuthorities().stream().map(Authority::getName)
			.anyMatch(authority -> authority.equals(AuthoritiesConstants.ADMIN));
    }
}
