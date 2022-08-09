package org.mitre.healthmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DateTimeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.domain.FHIRPatient;
import org.mitre.healthmanager.repository.FHIRClientRepository;
import org.mitre.healthmanager.repository.FHIRPatientRepository;
import org.mitre.healthmanager.service.dto.FHIRClientDTO;
import org.mitre.healthmanager.service.dto.FHIRPatientConsentDTO;
import org.mitre.healthmanager.service.dto.UserDTO;
import org.mitre.healthmanager.service.mapper.FHIRPatientConsentMapper;
import org.mitre.healthmanager.service.mapper.FHIRPatientConsentMapperImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.BundleProviders;

@ExtendWith(MockitoExtension.class)
class FHIRPatientConsentServiceTest {
	
    @Mock
	DaoRegistry myDaoRegistry;
    
    @InjectMocks
    FHIRPatientConsentMapper fhirPatientConsentMapper = new FHIRPatientConsentMapperImpl();
    
    @Mock
    FHIRPatientRepository fhirPatientRepository;
    
    @Mock
    FHIRClientRepository fhirClientRepository;
    
    @Mock
    IFhirResourceDao<Consent> consentDAO;
        
    FHIRPatientConsentService fhirPatientConsentService;
    
    @BeforeEach
    private void setup() {
    	fhirPatientConsentService = new FHIRPatientConsentService(myDaoRegistry, 
    			fhirPatientConsentMapper);
    }
    
    private FHIRPatientConsentDTO setupDefaultPatientClient() {
		FHIRPatient fhirPatient = new FHIRPatient();
		fhirPatient.setFhirId("fhirPatient");
		when(fhirPatientRepository.findOneForUser((long) 1234)).thenReturn(Optional.of(fhirPatient));
		
		FHIRClient fhirClient = new FHIRClient();
		fhirClient.setFhirOrganizationId("fhirClient");
		when(fhirClientRepository.getById((long) 5678)).thenReturn(fhirClient);
		
		FHIRPatientConsentDTO fhirPatientConsentDTO = new FHIRPatientConsentDTO();
		fhirPatientConsentDTO.setApprove(true);
		UserDTO userDTO = new UserDTO();
		userDTO.setId((long) 1234);
		fhirPatientConsentDTO.setUser(userDTO);
		FHIRClientDTO clientDTO = new FHIRClientDTO();
		clientDTO.setId((long) 5678);
		fhirPatientConsentDTO.setClient(clientDTO);
		
		return fhirPatientConsentDTO;
    }

	@Test
	void testSaveConsent() {
		ArgumentCaptor<Consent> consetCaptor = ArgumentCaptor.forClass(Consent.class);			
		DaoMethodOutcome resp = new DaoMethodOutcome().setCreated(true);
		resp.setResource(new Consent());
		when(myDaoRegistry.getResourceDao(Consent.class)).thenReturn(consentDAO);	
		when(consentDAO.create(any(Consent.class), any(RequestDetails.class))).thenReturn(resp);
		
		FHIRPatientConsentDTO fhirPatientConsentDTO = setupDefaultPatientClient();
		
		fhirPatientConsentService.save(fhirPatientConsentDTO);
		verify(consentDAO).create(consetCaptor.capture(), any(RequestDetails.class));
		assertNotNull(consetCaptor.getValue());
		Consent actualConsent = consetCaptor.getValue();
		assertEquals(Consent.ConsentState.ACTIVE, actualConsent.getStatus());
		assertEquals("fhirPatient", actualConsent.getPatient().getReferenceElement().getIdPart());
		assertEquals("urn:mitre:healthmanager", actualConsent.getOrganizationFirstRep().getIdentifier().getSystem());
		assertEquals(Consent.ConsentProvisionType.PERMIT, actualConsent.getProvision().getType());
		assertNotNull(actualConsent.getProvision().getPeriod().getStartElement());
		assertEquals(FHIRPatientConsentMapper.PROVISION_ACTOR_ROLE_VALUE, 
				actualConsent.getProvision().getActorFirstRep().getRole().getCodingFirstRep().getCode());
		assertEquals(FHIRPatientConsentMapper.PROVISION_ACTOR_ROLE_SYSTEM, 
				actualConsent.getProvision().getActorFirstRep().getRole().getCodingFirstRep().getSystem());
		assertEquals("fhirClient", actualConsent.getProvision().getActorFirstRep().getReference().getReferenceElement().getIdPart());
	}
	
	@Test
	void testUpdateToggleApproval() {
		DaoMethodOutcome resp = new DaoMethodOutcome().setCreated(true);
		resp.setResource(new Consent());
		when(myDaoRegistry.getResourceDao(Consent.class)).thenReturn(consentDAO);	
		when(consentDAO.update(any(Consent.class), any(RequestDetails.class))).thenReturn(resp);
		
		FHIRPatientConsentDTO fhirPatientConsentDTO = setupDefaultPatientClient();
		Consent consent = fhirPatientConsentMapper.toConsent(fhirPatientConsentDTO);
		consent.getProvision().setType(Consent.ConsentProvisionType.DENY);
		DateTimeType originalDateTimeType = DateTimeType.now();
		consent.getProvision().getPeriod().setStartElement(originalDateTimeType);
		
		IBundleProvider bundle = BundleProviders.newList(consent);
		when(myDaoRegistry.getResourceDao(Consent.class)).thenReturn(consentDAO);	
		when(consentDAO.search(any(SearchParameterMap.class), any(RequestDetails.class))).thenReturn(bundle);
		
		ArgumentCaptor<Consent> consetCaptor = ArgumentCaptor.forClass(Consent.class);
		fhirPatientConsentService.update(fhirPatientConsentDTO);
		verify(consentDAO).update(consetCaptor.capture(), any(RequestDetails.class));
		assertNotNull(consetCaptor.getValue());
		Consent actualConsent = consetCaptor.getValue();
		assertEquals(Consent.ConsentState.ACTIVE, actualConsent.getStatus());
		assertTrue(actualConsent.getProvision().getPeriod().getStartElement().after(originalDateTimeType));
	}

	@Test
	void testFindActiveByUser() {
		FHIRPatientConsentDTO fhirPatientConsentDTO = setupDefaultPatientClient();
		Consent consent = fhirPatientConsentMapper.toConsent(fhirPatientConsentDTO);
		IBundleProvider bundle = BundleProviders.newList(consent);
		when(myDaoRegistry.getResourceDao(Consent.class)).thenReturn(consentDAO);	
		when(consentDAO.search(any(SearchParameterMap.class), any(RequestDetails.class))).thenReturn(bundle);
		
		ArgumentCaptor<SearchParameterMap> captor = ArgumentCaptor.forClass(SearchParameterMap.class);
		fhirPatientConsentService.findActiveByUser(fhirPatientConsentDTO.getUser());
		verify(consentDAO).search(captor.capture(), any(RequestDetails.class));
		assertEquals("fhirPatient", fhirPatientConsentMapper.userToPatient(fhirPatientConsentDTO.getUser()).getReferenceElement().getIdPart());
	}
}
