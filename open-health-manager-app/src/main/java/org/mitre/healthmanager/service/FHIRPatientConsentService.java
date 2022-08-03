package org.mitre.healthmanager.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Period;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.mitre.healthmanager.service.mapper.FHIRPatientConsentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * Service Implementation for managing {@link FHIRPatientConsent}.
 */
@Service
@Transactional //use HAPI transaction manager
public class FHIRPatientConsentService {

    private final Logger log = LoggerFactory.getLogger(FHIRPatientConsentService.class);

	private DaoRegistry myDaoRegistry;
   
	private FHIRPatientConsentMapper fhirPatientConsentMapper;

    public FHIRPatientConsentService(DaoRegistry myDaoRegistry, 
    		FHIRPatientConsentMapper fhirPatientConsentMapper) {
        this.myDaoRegistry = myDaoRegistry;
        this.fhirPatientConsentMapper = fhirPatientConsentMapper;
    }

    /**
     * Save a fHIRPatientConsent.
     *
     * @param fHIRPatientConsentDTO the patient and client for which to create new consent reource to save.
     * @return the persisted resource.
     */
    public FHIRPatientConsentDTO save(FHIRPatientConsentDTO fHIRPatientConsentDTO) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsentDTO);
        Consent consent = fhirPatientConsentMapper.toConsent(fHIRPatientConsentDTO);      
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = resourceDAO.create(consent, requestDetails);
        if (!resp.getCreated()) {
            throw new RuntimeException("FHIR Consent creation failed");
        }
        fhirPatientConsentMapper.updateDto(fHIRPatientConsentDTO, (Consent) resp.getResource());            
        return fHIRPatientConsentDTO;
    }

    /**
     * Update a fHIRPatientConsent.
     *
     * @param fHIRPatientConsentDTO the resource to save.
     * @return the persisted resource.
     */
    public FHIRPatientConsentDTO update(FHIRPatientConsentDTO fHIRPatientConsentDTO) {
        log.debug("Request to save FHIRPatientConsent : {}", fHIRPatientConsentDTO);
        
        Consent consent;
        if(Objects.isNull(fHIRPatientConsentDTO.getFhirResource())) {
        	consent = findActiveConsentByUserAndClient(fHIRPatientConsentDTO.getUser(), fHIRPatientConsentDTO.getClient())
        		.get().stream().findFirst().get();
        }else {
        	consent = (Consent) FhirContext.forR4().newJsonParser().parseResource(fHIRPatientConsentDTO.getFhirResource());
        }
        
    	if(consent.getProvision() != null &&
    			consent.getProvision().getTypeElement() != null){
        	Boolean consentApprove = consent.getProvision().getType().equals(Consent.ConsentProvisionType.PERMIT);	
        	if(consentApprove != fHIRPatientConsentDTO.getApprove()) {
        		//change in approve/deny, set period start
        		consent.getProvision().setType(fHIRPatientConsentDTO.getApprove() ? Consent.ConsentProvisionType.PERMIT : Consent.ConsentProvisionType.DENY);
        		consent.getProvision().setPeriod(new Period().setStartElement(DateTimeType.now()));
        	}
    	}
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = resourceDAO.update(consent, requestDetails);
        if (!resp.getCreated()) {
            throw new RuntimeException("FHIR Consent creation failed");
        }
        fhirPatientConsentMapper.updateDto(fHIRPatientConsentDTO, (Consent) resp.getResource()); 
        return fHIRPatientConsentDTO;
    }
    
    /**
    * Get all the fHIRPatientConsents with eager load of many-to-many relationships.
    *
    * @return the list of entities.
    */
    public Page<FHIRPatientConsentDTO> findAllWithEagerRelationships(Pageable pageable) {
        log.debug("Request to get all FHIRPatientConsents");
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        // reference:identifier search does not seem to work, for now manually filter
        //SearchParameterMap searchParameterMap = new SearchParameterMap("organization", new ReferenceParam("identifier", "urn:mitre:healthmanager|"));
        		//.add(Constants.PARAM_OFFSET, new NumberParam(pageable.getPageNumber())) not available
        		//.add(Constants.PARAM_COUNT, new NumberParam(pageable.getPageSize()));
        SearchParameterMap searchParameterMap = new SearchParameterMap();
        IBundleProvider results = resourceDAO.search(searchParameterMap, requestDetails);
        
        List<FHIRPatientConsentDTO> pageList = results.getAllResources()
        		.stream()
        		.map(resource -> (Consent) resource)
        		.filter(consent -> consent.getOrganizationFirstRep() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem().equals("urn:mitre:healthmanager"))
        	    .skip(pageable.getPageSize() * pageable.getPageNumber())
        	    .limit(pageable.getPageSize())
        	    .map(consent -> {
        	    	return fhirPatientConsentMapper.toDtoEagerLoad(consent);          	    	
        	    })
        	    .collect(Collectors.toList());

        return new PageImpl<>(pageList, pageable, results.getAllResources().size());
    }


    /**
     * Get all the fHIRPatientConsents.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<FHIRPatientConsentDTO> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRPatientConsents");
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        SearchParameterMap searchParameterMap = new SearchParameterMap();
        IBundleProvider results = resourceDAO.search(searchParameterMap, requestDetails);
        
        List<FHIRPatientConsentDTO> pageList = results.getAllResources()
        		.stream()
        		.map(resource -> (Consent) resource)
        		.filter(consent -> consent.getOrganizationFirstRep() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem().equals("urn:mitre:healthmanager"))
        	    .skip(pageable.getPageSize() * pageable.getPageNumber())
        	    .limit(pageable.getPageSize())
        	    .map(consent -> {
        	    	return fhirPatientConsentMapper.toDto(consent);          	    	
        	    })
        	    .collect(Collectors.toList());

        return new PageImpl<>(pageList, pageable, results.getAllResources().size());
    }

    /**
     * Get one fHIRPatientConsent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<FHIRPatientConsentDTO> findOne(String id) {
        log.debug("Request to get FHIRPatientConsent : {}", id);
    	IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
    	Consent consent = null;
    	try {
    		consent = resourceDAO.read(new IdType(id));
    	} catch(ResourceNotFoundException rnfe) {    		
			throw new RuntimeException("Consent resource does not exist.");
		}
    	
    	FHIRPatientConsentDTO consentDTO = fhirPatientConsentMapper.toDtoEagerLoad(consent);  
    	
    	return Optional.of(consentDTO);
    }

    /**
     * Delete the fHIRPatientConsent resource by id.
     *
     * @param id the id of the resource.
     */
    public void delete(String id) {
        log.debug("Request to delete FHIRPatientConsent resource : {}", id);
    	IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
    	resourceDAO.delete(new IdType(id));
    }   
    
    /**
     * Get one fHIRPatientConsent by user and client.
     *
     * @param userDTO
     * @param fhirClientDTO
     * @return the entity.
     */
    public Optional<List<FHIRPatientConsentDTO>> findActiveByUser(UserDTO userDTO) {
        log.debug("Request to get active FHIRPatientConsent for user: {}", userDTO);
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        // reference:identifier search does not seem to work, for now manually filter
        //SearchParameterMap searchParameterMap = new SearchParameterMap("organization", new ReferenceParam("identifier", "urn:mitre:healthmanager|"));
        SearchParameterMap searchParameterMap = new SearchParameterMap();
        searchParameterMap.add("active", new StringParam("true"));
        searchParameterMap.add("patient", new ReferenceParam().setValue(fhirPatientConsentMapper.userToPatient(userDTO).getReference()));
        IBundleProvider results = resourceDAO.search(searchParameterMap, requestDetails);
        
        List<FHIRPatientConsentDTO> filteredList = results.getAllResources()
        		.stream()
        		.map(resource -> (Consent) resource)
        		.filter(consent -> consent.getOrganizationFirstRep() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem().equals("urn:mitre:healthmanager"))
        	    .map(consent -> {
        	    	return fhirPatientConsentMapper.toDtoEagerLoad(consent);          	    	
        	    })
        	    .collect(Collectors.toList());  
    	
    	return Optional.of(filteredList);
    }
    
    private Optional<List<Consent>> findActiveConsentByUserAndClient(UserDTO userDTO, FHIRClientDTO fhirClientDTO) {
        log.debug("Request to get active FHIRPatientConsent for user: {} by client", userDTO);
        IFhirResourceDao<Consent> resourceDAO = myDaoRegistry.getResourceDao(Consent.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        SearchParameterMap searchParameterMap = new SearchParameterMap();
        searchParameterMap.add("active", new StringParam("true"));
        searchParameterMap.add("patient", new ReferenceParam().setValue(fhirPatientConsentMapper.userToPatient(userDTO).getReference()));
        IBundleProvider results = resourceDAO.search(searchParameterMap, requestDetails);
        
        String fhirOrganizationId = fhirPatientConsentMapper.fhirClientToReference(fhirClientDTO).getReferenceElement().getIdPart();
        
        List<Consent> filteredList = results.getAllResources()
        		.stream()
        		.map(resource -> (Consent) resource)
        		.filter(consent -> consent.getOrganizationFirstRep() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem() != null)
        		.filter(consent -> consent.getOrganizationFirstRep().getIdentifier().getSystem().equals("urn:mitre:healthmanager"))
        		.filter(consent -> consent.getProvision() != null)
        		.filter(consent -> consent.getProvision().getActorFirstRep() != null)
        		.filter(consent -> consent.getProvision().getActorFirstRep().getReference().getReferenceElement().getIdPart().equals(fhirOrganizationId))
        	    .collect(Collectors.toList());  
    	
    	return Optional.of(filteredList);
    }
}
