package org.mitre.healthmanager.service;

import java.util.Optional;

import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
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
    
    public FHIRPatient createFHIRPatientForUser(User user) {
    	// check if FHIRPatient record already exists for user
    	FHIRPatient patient = findOneForUser(user.getId()).orElse(null);
    	if(patient != null) {
    		return patient;
    	}
        checkFHIRLogin(user.getLogin());

        // create the patient
        Patient patientFHIR = new Patient();
        patientFHIR.addIdentifier()
            .setSystem(FHIR_LOGIN_SYSTEM)
            .setValue(user.getLogin());
        patientFHIR.addName()
            .setFamily(user.getLastName())
            .addGiven(user.getFirstName());
        
        // DaoMethodOutcome resp = patientDAO.create(patientFHIR); //does not fire interceptors
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        RequestDetails requestDetails = SystemRequestDetails.forAllPartition();
        DaoMethodOutcome resp = patientDAO.create(patientFHIR, requestDetails); //fires interceptors
        // JpaResourceProviderR4<Patient> patientProvider = new JpaResourceProviderR4<Patient>(patientDAO); 
        // MethodOutcome resp = patientProvider.create(null, patientFHIR, null, requestDetails); //fires interceptors
        if (!resp.getCreated()) {
            throw new RuntimeException("FHIR Patient creation failed");
        }

        patient = new FHIRPatient();
        patient.fhirId(resp.getId().getIdPart());
        patient.user(user);
        fHIRPatientRepository.save(patient);

        log.debug("linked to FHIR patient id: {}", patient.getFhirId());

        return patient;
    }
    
    private void checkFHIRLogin(String targetUsername) {
        IFhirResourceDao<Patient> patientDAO = myDaoRegistry.getResourceDao(Patient.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResults = 
            patientDAO.search(
                new SearchParameterMap(
                    "identifier", 
                    new TokenParam(FHIR_LOGIN_SYSTEM, targetUsername)
                ),
                searchRequestDetails
            );
        if (!searchResults.isEmpty()) {
            throw new UsernameAlreadyUsedFHIRException();
        }
    }
}
