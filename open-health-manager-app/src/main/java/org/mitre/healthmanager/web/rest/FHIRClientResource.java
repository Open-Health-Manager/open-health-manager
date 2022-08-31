package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.mitre.healthmanager.service.FHIRClientService;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.mysql.cj.util.StringUtils;

import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRClient}.
 */
@RestController
@RequestMapping("/api")
public class FHIRClientResource {

    private final Logger log = LoggerFactory.getLogger(FHIRClientResource.class);
    
    private static final String ENTITY_NAME = "fHIRClient";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FHIRClientService fHIRClientService;
    
    private final FHIRClientRepository fHIRClientRepository;

    public FHIRClientResource(FHIRClientService fHIRClientService, FHIRClientRepository fHIRClientRepository) {
        this.fHIRClientService = fHIRClientService;
        this.fHIRClientRepository = fHIRClientRepository;
    }


    /**
     * {@code GET  /fhir-clients} : get all the fHIRClients.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of fHIRClients in body.
     */
    @GetMapping("/fhir-clients")
    public ResponseEntity<List<FHIRClientDTO>> getAllFHIRClients(@org.springdoc.api.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of FHIRClients");
        Page<FHIRClientDTO> page = fHIRClientService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /fhir-clients/:id} : get the "id" fHIRClient.
     *
     * @param id the id of the fHIRClientDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the fHIRClientDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/fhir-clients/{id}")
    public ResponseEntity<FHIRClientDTO> getFHIRClient(@PathVariable Long id) {
        log.debug("REST request to get FHIRClient : {}", id);
        Optional<FHIRClientDTO> fHIRClientDTO = fHIRClientService.findOne(id);
        return ResponseUtil.wrapOrNotFound(fHIRClientDTO);
    }
    
    /**
     * {@code POST  /admin/fhir-clients} : Create a new fHIRClient.
     *
     * @param fHIRClientDTO the fHIRClientDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new fHIRClientDTO, or with status {@code 400 (Bad Request)} if the fHIRClient has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/admin/fhir-clients")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRClientDTO> createFHIRClient(@Valid @RequestBody FHIRClientDTO fHIRClientDTO) throws URISyntaxException {
        log.debug("REST request to save FHIRClient : {}", fHIRClientDTO);
        if (fHIRClientDTO.getId() != null) {
            throw new BadRequestAlertException("A new fHIRClient cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FHIRClientDTO result = fHIRClientService.save(fHIRClientDTO);
        return ResponseEntity
            .created(new URI("/api/admin/fhir-clients/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /admin/fhir-clients/:id} : Updates an existing fHIRClient.
     *
     * @param id the id of the fHIRClientDTO to save.
     * @param fHIRClientDTO the fHIRClientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fHIRClientDTO,
     * or with status {@code 400 (Bad Request)} if the fHIRClientDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the fHIRClientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/admin/fhir-clients/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRClientDTO> updateFHIRClient(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody FHIRClientDTO fHIRClientDTO
    ) throws URISyntaxException {
        log.debug("REST request to update FHIRClient : {}, {}", id, fHIRClientDTO);
        if (fHIRClientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fHIRClientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (StringUtils.isNullOrEmpty(fHIRClientDTO.getFhirOrganizationId())) {
            throw new BadRequestAlertException("Invalid FHIR Organization ID", ENTITY_NAME, "fhirorgidnull");
        }

        if (!fHIRClientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        FHIRClientDTO result = fHIRClientService.update(fHIRClientDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRClientDTO.getId().toString()))
            .body(result);
    }

    /**
     * {@code PATCH  /admin/fhir-clients/:id} : Partial updates given fields of an existing fHIRClient, field will ignore if it is null
     *
     * @param id the id of the fHIRClientDTO to save.
     * @param fHIRClientDTO the fHIRClientDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated fHIRClientDTO,
     * or with status {@code 400 (Bad Request)} if the fHIRClientDTO is not valid,
     * or with status {@code 404 (Not Found)} if the fHIRClientDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the fHIRClientDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/admin/fhir-clients/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<FHIRClientDTO> partialUpdateFHIRClient(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody FHIRClientDTO fHIRClientDTO
    ) throws URISyntaxException {
        log.debug("REST request to partial update FHIRClient partially : {}, {}", id, fHIRClientDTO);
        if (fHIRClientDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, fHIRClientDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!fHIRClientRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<FHIRClientDTO> result = fHIRClientService.partialUpdate(fHIRClientDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, fHIRClientDTO.getId().toString())
        );
    }

    /**
     * {@code DELETE  /admin/fhir-clients/:id} : delete the "id" fHIRClient.
     *
     * @param id the id of the fHIRClientDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/admin/fhir-clients/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteFHIRClient(@PathVariable Long id) {
        log.debug("REST request to delete FHIRClient : {}", id);
        fHIRClientService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
