package org.mitre.healthmanager.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.domain.User;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class FHIRPatientServiceTest {
	private FHIRPatientService fhirPatientService;
	
	@Mock
	private FHIRPatientRepository fHIRPatientRepository;
	
	@Mock
	private DaoRegistry myDaoRegistry;
	
	@Mock
	private IFhirResourceDao<Patient> patientDAO;
	
	@BeforeEach
    public void setup() {
		fhirPatientService = new FHIRPatientService(fHIRPatientRepository);
		ReflectionTestUtils.setField(fhirPatientService, "myDaoRegistry", myDaoRegistry);
	}

	@Test
	void testSaveFHIRPatientNoChanges() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("test");
		fhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(fhirPatient));
		when(fHIRPatientRepository.save(any(FHIRPatient.class))).thenReturn(fhirPatient);
				
		FHIRPatient result = fhirPatientService.save(fhirPatient);
		assertNotNull(result);
	}
	
	@Test
	void testSaveFHIRPatientExistingPatientResource() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		Patient patient = new Patient();
		patient.addIdentifier()
    		.setSystem(FHIRPatientService.FHIR_LOGIN_SYSTEM)
    		.setValue(user.getLogin());
		patient.setId(existingFhirPatient.getFhirId());
		IBundleProvider  bundleProvider = new SimpleBundleProvider(patient);
		when(patientDAO.search(any(SearchParameterMap.class), any())).thenReturn(bundleProvider);
				
		Exception exception = assertThrows(FHIRPatientResourceException.class, () -> fhirPatientService.save(fhirPatient));
		assertEquals("Existing link between patient resource and user account.", exception.getMessage());		
	}
	
	@Test
	void testSaveFHIRPatientUserHasSamePatientResource() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		Patient patient = new Patient();
		patient.addIdentifier()
    		.setSystem(FHIRPatientService.FHIR_LOGIN_SYSTEM)
    		.setValue(user.getLogin());
		patient.setId(fhirPatient.getFhirId());
		when(patientDAO.search(any(SearchParameterMap.class), any()))
			.thenReturn(new SimpleBundleProvider())
			.thenReturn(new SimpleBundleProvider(patient));
		when(patientDAO.read(any(IdType.class)))
			.thenReturn(patient);
		DaoMethodOutcome dmo = new DaoMethodOutcome();
		dmo.setCreated(true);
		dmo.setId(new IdType(fhirPatient.getFhirId()));
		when(patientDAO.create(any(Patient.class), any(RequestDetails.class)))
			.thenReturn(dmo);				
		when(fHIRPatientRepository.save(any(FHIRPatient.class))).thenReturn(fhirPatient);
		
		FHIRPatient result = fhirPatientService.save(fhirPatient);
		assertNotNull(result);
	}
	
	@Test
	void testSaveFHIRPatientUserHasOtherPatientResource() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		Patient patient = new Patient();
		patient.addIdentifier()
    		.setSystem(FHIRPatientService.FHIR_LOGIN_SYSTEM)
    		.setValue(user.getLogin());
		patient.setId("otherPatientId");
		when(patientDAO.search(any(SearchParameterMap.class), any()))
			.thenReturn(new SimpleBundleProvider())
			.thenReturn(new SimpleBundleProvider(patient));
				
		Exception exception =  assertThrows(FHIRPatientResourceException.class, () -> fhirPatientService.save(fhirPatient));
		assertEquals("User account already linked to another patient resource.", exception.getMessage());
	}
	
	@Test
	void testSaveFHIRPatientPatientResourceDoesNotExist() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		when(patientDAO.search(any(SearchParameterMap.class), any()))
			.thenReturn(new SimpleBundleProvider());
		when(patientDAO.read(any(IdType.class))).thenThrow(new ResourceNotFoundException(""));

		Exception exception = assertThrows(FHIRPatientResourceException.class, () -> fhirPatientService.save(fhirPatient));
		assertEquals("Patient resource does not exist.", exception.getMessage());
	}
	
	@Test
	void testSaveFHIRPatientPatientResourceHasSameUser() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		when(patientDAO.search(any(SearchParameterMap.class), any()))
			.thenReturn(new SimpleBundleProvider());
		Patient patient = new Patient();
		patient.addIdentifier()
    		.setSystem(FHIRPatientService.FHIR_LOGIN_SYSTEM)
    		.setValue(user.getLogin());
		patient.setId(fhirPatient.getFhirId());
		when(patientDAO.read(any(IdType.class)))
			.thenReturn(patient);
		DaoMethodOutcome dmo = new DaoMethodOutcome();
		dmo.setCreated(true);
		dmo.setId(new IdType("patientId"));
		when(patientDAO.create(any(Patient.class), any(RequestDetails.class)))
			.thenReturn(dmo);				
		when(fHIRPatientRepository.save(any(FHIRPatient.class))).thenReturn(fhirPatient);
		
		FHIRPatient result = fhirPatientService.save(fhirPatient);
		assertNotNull(result);
	}
	
	@Test
	void testSaveFHIRPatientPatientResourceHasOtherUser() {
		FHIRPatient fhirPatient = new FHIRPatient();
		User user = new User();
		user.setId(Long.valueOf(12345));
		user.setLogin("userLogin");
		fhirPatient.setId(Long.valueOf(12345));
		fhirPatient.setFhirId("patientId");
		fhirPatient.setUser(user);
		
		FHIRPatient existingFhirPatient = new FHIRPatient();
		existingFhirPatient.setId(Long.valueOf(12345));
		existingFhirPatient.setFhirId("oldPatientId");
		existingFhirPatient.setUser(user);
		
		when(fHIRPatientRepository.findById(Long.valueOf(12345))).thenReturn(Optional.of(existingFhirPatient));
		when(myDaoRegistry.getResourceDao(Patient.class)).thenReturn(patientDAO);	
		when(patientDAO.search(any(SearchParameterMap.class), any()))
			.thenReturn(new SimpleBundleProvider());
		Patient patient = new Patient();
		patient.addIdentifier()
    		.setSystem(FHIRPatientService.FHIR_LOGIN_SYSTEM)
    		.setValue("otherUser");
		patient.setId(fhirPatient.getFhirId());
		when(patientDAO.read(any(IdType.class)))
			.thenReturn(patient);
				
		Exception exception = assertThrows(FHIRPatientResourceException.class, () -> fhirPatientService.save(fhirPatient));
		assertEquals("Patient resource already linked to another user account.", exception.getMessage());
	}

}
