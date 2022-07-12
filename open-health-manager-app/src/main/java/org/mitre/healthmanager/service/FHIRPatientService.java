package org.mitre.healthmanager.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.domain.Authority;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.security.AuthoritiesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

/**
 * Service Implementation for managing {@link FHIRPatient}.
 */
@Service
@Transactional("jhipsterTransactionManager")
public class FHIRPatientService {
    public static final String FHIR_LOGIN_SYSTEM = "urn:mitre:healthmanager:account:username";
    
    private final Logger log = LoggerFactory.getLogger(FHIRPatientService.class);

    private final FHIRPatientRepository fHIRPatientRepository;
    
    @Autowired
	private DaoRegistry myDaoRegistry;

    public FHIRPatientService(FHIRPatientRepository fHIRPatientRepository) {
        this.fHIRPatientRepository = fHIRPatientRepository;
    }

    /**
     * Save a fHIRPatient.
     *
     * @param fHIRPatient the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatient save(FHIRPatient fHIRPatient) {
        log.debug("Request to save FHIRPatient : {}", fHIRPatient);
        saveFHIRPatient(fHIRPatient);
        return fHIRPatientRepository.save(fHIRPatient);
    }

    /**
     * Update a fHIRPatient.
     *
     * @param fHIRPatient the entity to save.
     * @return the persisted entity.
     */
    public FHIRPatient update(FHIRPatient fHIRPatient) {
        log.debug("Request to save FHIRPatient : {}", fHIRPatient);
        saveFHIRPatient(fHIRPatient);
        return fHIRPatientRepository.save(fHIRPatient);
    }

    /**
     * Partially update a fHIRPatient.
     *
     * @param fHIRPatient the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FHIRPatient> partialUpdate(FHIRPatient fHIRPatient) {
        log.debug("Request to partially update FHIRPatient : {}", fHIRPatient);

        return fHIRPatientRepository
            .findById(fHIRPatient.getId())
            .map(existingFHIRPatient -> {
                if (fHIRPatient.getFhirId() != null) {
                    existingFHIRPatient.setFhirId(fHIRPatient.getFhirId());
                }

                saveFHIRPatient(existingFHIRPatient);
                return existingFHIRPatient;
            })
            .map(fHIRPatientRepository::save);
    }

    /**
     * Get all the fHIRPatients.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(transactionManager = "jhipsterTransactionManager", readOnly = true)
    public Page<FHIRPatient> findAll(Pageable pageable) {
        log.debug("Request to get all FHIRPatients");
        return fHIRPatientRepository.findAll(pageable);
    }

    /**
     * Get all the fHIRPatients with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<FHIRPatient> findAllWithEagerRelationships(Pageable pageable) {
        return fHIRPatientRepository.findAllWithEagerRelationships(pageable);
    }

    /**
     * Get one fHIRPatient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(transactionManager = "jhipsterTransactionManager", readOnly = true)
    public Optional<FHIRPatient> findOne(Long id) {
        log.debug("Request to get FHIRPatient : {}", id);
        return fHIRPatientRepository.findOneWithEagerRelationships(id);
    }
    
    /**
     * Get one fHIRPatient by User id.
     *
     * @param id the id of the User entity.
     * @return the entity.
     */
    @Transactional(transactionManager = "jhipsterTransactionManager", readOnly = true)
    public Optional<FHIRPatient> findOneForUser(Long id) {
        log.debug("Request to get FHIRPatient by User : {}", id);
        return fHIRPatientRepository.findOneForUser(id);
    }

    /**
     * Delete the fHIRPatient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete FHIRPatient : {}", id);
        fHIRPatientRepository.deleteById(id);
    }
    
    
    /**
     * Creates FHIRPatient record for user if not already existing, user is ROLE_USER and user account is activated.
     * If record already exists, will return existing record.
     * Throws exception if there is an existing FHIR patient resource for the user.
     * 
     * @param user User entity
     * @return the entity or null
     */
    public FHIRPatient createFHIRPatientForUser(User user) {
    	// check if FHIRPatient record already exists for user
    	FHIRPatient fhirPatient = findOneForUser(user.getId()).orElse(null);
    	if(fhirPatient != null) {
    		return fhirPatient;
    	}
    	// do not create FHIR Patient if user not activated or not ROLE_USER
    	if(!user.isActivated() || 
    			user.getAuthorities().stream().map(Authority::getName)
    			.noneMatch(authority -> authority.equals(AuthoritiesConstants.USER))) {
    		return null;
    	}

        checkFHIRLogin(user);

        // create the patient
        Patient patientFHIR = new Patient();
        patientFHIR.addName()
        	.setFamily(user.getLastName())
        	.addGiven(user.getFirstName());
        fhirPatient = new FHIRPatient();
        fhirPatient.fhirId(savePatientResource(patientFHIR, user));
        fhirPatient.user(user);
        fHIRPatientRepository.save(fhirPatient);

        log.debug("linked to FHIR patient id: {}", fhirPatient.getFhirId());

        return fhirPatient;
    }
    
    
    // internal save or update
    // need to manually unlink patient resource identifiers for now in case of failures
    private void saveFHIRPatient(FHIRPatient fHIRPatient) {
    	if(fHIRPatient.getId() != null) { // existing record
        	FHIRPatient existingFHIRPatient = fHIRPatientRepository
        			.findById(fHIRPatient.getId()).get();
        	User existingUser = existingFHIRPatient.getUser();
        	
        	if(existingFHIRPatient.getFhirId().equals(fHIRPatient.getFhirId()) 
        			&& existingUser.getId().equals(fHIRPatient.getUser().getId())) {
        		// no changes
        		return;
        	}
        	
        	// fail if existing record has a patient resource link already    		
            if (getExistingFhirPatientResources(existingFHIRPatient, existingUser).size() > 0) {
                throw new FHIRPatientResourceException("Existing link between patient resource and user account.");
            }
    	}
    	
    	// fail if user account has any other linked patient resources
    	if(getExistingFhirPatientResources(null, fHIRPatient.getUser()).stream()
    			.filter(resource -> !resource.getIdElement().getIdPart().equals(fHIRPatient.getFhirId()))
    			.count() > 0) {
    		throw new FHIRPatientResourceException("User account already linked to another patient resource.");
    	}
    	
    	// fail if patient resource does not exist
    	IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
    	Patient patientFHIR = null;
    	try {
    		patientFHIR = patientDAO.read(new IdType(fHIRPatient.getFhirId()));
    	} catch(ResourceNotFoundException rnfe) {    		
			throw new FHIRPatientResourceException("Patient resource does not exist.");
		}
		
		// fail if patient resource linked to another user account
		if(hasExistingAccountIdentifier(patientFHIR, fHIRPatient.getUser())) {
			throw new FHIRPatientResourceException("Patient resource already linked to another user account.");
		}
		
		savePatientResource(patientFHIR, fHIRPatient.getUser());
    }
    
    private String savePatientResource(Patient patientFHIR, User user) {
    	if(patientFHIR.getIdentifier().stream()
				.filter(identifier -> identifier.getSystem().equals(FHIR_LOGIN_SYSTEM) 
						&& identifier.getValue().equals(user.getLogin()))
				.count() == 0) {
            patientFHIR.addIdentifier()
            	.setSystem(FHIR_LOGIN_SYSTEM)
            	.setValue(user.getLogin());		
    	}
        
        // DaoMethodOutcome resp = patientDAO.create(patientFHIR); //does not fire interceptors
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = patientDAO.create(patientFHIR, requestDetails); //fires interceptors
        // JpaResourceProviderR4<Patient> patientProvider = new JpaResourceProviderR4<Patient>(patientDAO); 
        // MethodOutcome resp = patientProvider.create(null, patientFHIR, null, requestDetails); //fires interceptors
        if (!resp.getCreated()) {
            throw new RuntimeException("FHIR Patient creation failed");
        }
        
        return resp.getId().getIdPart();
    }
    
    private void checkFHIRLogin(User user) {
        if (getExistingFhirPatientResources(null, user).size() > 0) {
            throw new UsernameAlreadyUsedFHIRException();
        }
    }
    
    private List<IBaseResource> getExistingFhirPatientResources(FHIRPatient fHIRPatient, User user) {
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResults = 
            patientDAO.search(
                new SearchParameterMap(
                    "identifier", 
                    new TokenParam(FHIR_LOGIN_SYSTEM, user.getLogin())
                ),
                searchRequestDetails
            );
        if (!searchResults.isEmpty()) {
        	if(fHIRPatient == null) return searchResults.getAllResources();
        	return searchResults.getAllResources().stream()
        			.filter(resource -> resource.getIdElement().getIdPart().equals(fHIRPatient.getFhirId()))
        			.collect(Collectors.toList());
        }
        return searchResults.getAllResources();
    }
    
    // check if Patient resource has an account identifier for another user
    private boolean hasExistingAccountIdentifier(Patient patientFHIR, User user) {
    	return patientFHIR != null && 
    		patientFHIR.getIdentifier().stream()
				.filter(identifier -> identifier.getSystem().equals(FHIR_LOGIN_SYSTEM) 
						&& !identifier.getValue().equals(user.getLogin()))
				.count() > 0;
    }
}
