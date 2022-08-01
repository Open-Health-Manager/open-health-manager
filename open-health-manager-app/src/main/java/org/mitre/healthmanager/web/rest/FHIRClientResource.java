package org.mitre.healthmanager.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.service.FHIRClientService;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
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
 * REST controller for managing {@link org.mitre.healthmanager.domain.FHIRClient}.
 */
@RestController
@RequestMapping("/api")
public class FHIRClientResource {

    private final Logger log = LoggerFactory.getLogger(FHIRClientResource.class);

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FHIRClientService fHIRClientService;

    public FHIRClientResource(FHIRClientService fHIRClientService, FHIRClientRepository fHIRClientRepository) {
        this.fHIRClientService = fHIRClientService;
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
}
