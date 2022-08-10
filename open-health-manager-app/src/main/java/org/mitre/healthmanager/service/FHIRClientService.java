package org.mitre.healthmanager.service;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;


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
import org.springframework.beans.factory.annotation.Autowired;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;


/**
 * Service Implementation for managing {@link FHIRClient}.
 */
@Service
@Transactional
public class FHIRClientService {

    private final Logger log = LoggerFactory.getLogger(FHIRClientService.class);

    public static final String FHIR_CLIENT_SYSTEM = "urn:mitre:healthmanager:client:id";
    
    @Autowired
	private DaoRegistry myDaoRegistry;

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

        // create the organization resource
        Organization organizationFHIR = new Organization();
        organizationFHIR.setName(fHIRClient.getName());

        if (fHIRClient.getFhirOrganizationId() != null && fHIRClient.getFhirOrganizationId().length() > 0) {
            updateFHIROrganization(fHIRClient);
        } else {
            fHIRClient.fhirOrganizationId(saveOrganizationResource(organizationFHIR, fHIRClient));
        }
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
        fHIRClient.fhirOrganizationId(updateFHIROrganization(fHIRClient));
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
                existingFHIRClient.fhirOrganizationId(updateFHIROrganization(existingFHIRClient));
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
        FHIRClientDTO fhirClientDTO = new FHIRClientDTO();
        Optional<FHIRClientDTO> fhirClient = fHIRClientRepository.findById(id).map(fHIRClientMapper::toDto);
        if (fhirClient.isPresent()) {
            fhirClientDTO = fhirClient.get();
        }

        IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);

    	try {
    		organizationDAO.delete(new IdType(fhirClientDTO.getFhirOrganizationId()));
    	} catch(ResourceNotFoundException rnfe) {    		
			throw new FHIROrganizationResourceException("Organization resource does not exist.");
		}

        log.debug("Request to delete FHIRClient : {}", id);
        fHIRClientRepository.deleteById(id);
    }

    // Update the FHIR Organization if the FHIR client's name differs from the FHIR Organization's name
    private String updateFHIROrganization(FHIRClient fhirClient) {
    	if(fhirClient.getFhirOrganizationId() != null) { // existing record  	
        	// fail if fhirOrganizationID is linked to another client resource already  
            if (fHIRClientRepository.findAllForFhirOrganizationID(fhirClient.getFhirOrganizationId()).stream()
            .filter(client -> !client.getId().equals(fhirClient.getId()))
    		.count() > 0) {
                throw new FHIROrganizationResourceException("Existing link between organization resource and a different fhir client.");
            }
            
    	}
    	
    	// fail if organization resource does not exist
    	IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);
    	Organization organizationFHIR = null;
    	try {
    		organizationFHIR = organizationDAO.read(new IdType(fhirClient.getFhirOrganizationId()));
    	} catch(ResourceNotFoundException rnfe) {    		
			throw new FHIROrganizationResourceException("Organization resource does not exist.");
		}

        if (organizationFHIR.getName() != fhirClient.getName()) {
            organizationFHIR.setName(fhirClient.getName());
            return updateOrganizationResource(organizationFHIR, fhirClient);
        }

        return fhirClient.getFhirOrganizationId();
    }

    private String updateOrganizationResource(Organization organizationFHIR, FHIRClient fhirClient) {
        
        IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = null;
        try {
            resp = organizationDAO.update(organizationFHIR, requestDetails); //fires interceptors
        } catch(Exception e) {
            throw new FHIROrganizationResourceException("Organization resource does not exist.");
        }
        return resp.getId().getIdPart();
    }



    private String saveOrganizationResource(Organization organizationFHIR, FHIRClient fhirClient) {
        
        IFhirResourceDao<Organization> organizationDAO = myDaoRegistry.getResourceDao(Organization.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = organizationDAO.create(organizationFHIR, requestDetails); //fires interceptors
        if (!resp.getCreated()) {
            throw new FHIROrganizationResourceException("FHIR Organization creation failed");
        }
        
        return resp.getId().getIdPart();
    }

}
