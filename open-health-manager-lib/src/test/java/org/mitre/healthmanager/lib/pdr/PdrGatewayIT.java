package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes={TestApplication.class})
@TestPropertySource(properties = {
		"spring.batch.job.enabled=false",
		"spring.jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
		"hapi.fhir.enable_repository_validating_interceptor=true",
		"hapi.fhir.fhir_version=r4",
		"hapi.fhir.subscription.websocket_enabled=true",
		"hapi.fhir.mdm_enabled=true",
		// Override is currently required when using MDM as the construction of the MDM
		// beans are ambiguous as they are constructed multiple places. This is evident
		// when running in a spring boot environment
		"spring.main.allow-bean-definition-overriding=true",
		"hapi.fhir.allow_external_references=true"})
@Transactional
class PdrGatewayIT {
	@Autowired
	FhirContext ourCtx;
	
    @LocalServerPort
    private int port = 0;
    
    private IGenericClient testClient;
    
	@BeforeEach
	private void addPatient() {
		if(testClient == null) {
			ourCtx.getRestfulClientFactory().setSocketTimeout(200 * 1000);			
			testClient = ourCtx.newRestfulGenericClient("http://localhost:" + port + "/fhir/");
		}
		
		Patient patient = new Patient();
		patient.setId(new IdType("pat1"));		
		
		MethodOutcome mo = testClient.update().resource(patient).execute();
		if(mo.getId() == null) {
			throw new RuntimeException();
		}
	}

	@Test
	@Transactional
	void testProcessMessage() {							
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_valid.json")
        );
				
		Bundle response = processMessage(testMessage);
		assertSuccessResponse(response);
		assertExistsList(testMessage, response);
	}
	
	@Test
	@Transactional
	void testProcessMessageUnsupportedMessageEvent() {
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_valid.json")
        );
		MessageHeader header = (MessageHeader) testMessage.getEntryFirstRep().getResource();
		header.getEventUriType().setValue("unsupported");
		
		Bundle response = processMessage(testMessage);
		assertFailureResponse(response);
	}
	
	@Test
	@Transactional
	void testProcessHealthKitMessage() {
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_AppleHealthKit.json")
        );
		
		Bundle response = processMessage(testMessage);
		assertSuccessResponse(response);
		assertExistsList(testMessage, response);
	}
	
	@Test
	@Transactional
	void testProcessNoMessageIdNoFullUrlsMessage() {
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_noFullUrls.json")
        );
		
		Bundle bundle = testClient.search()
				.forResource("List")
				.returnBundle(Bundle.class).execute();	
		long listSizePre = bundle.getEntry().size();
		
		Bundle response = processMessage(testMessage);
		assertFailureResponse(response);
		
		bundle = testClient.search()
				.forResource("List")
				.returnBundle(Bundle.class).execute();	
		long listSizePost = bundle.getEntry().size();
		
		Assertions.assertThat(listSizePost).isEqualTo(listSizePre);
	}
	
	private Bundle processMessage(Bundle testMessage) {		
		return testClient
                .operation()
                .processMessage()
                .setMessageBundle(testMessage)
                .synchronous(Bundle.class)
                .execute();
	}
	
	private void assertSuccessResponse(IBaseBundle result) {
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).isInstanceOf(Bundle.class);
		Assertions.assertThat(((Bundle)result).getEntryFirstRep()).isNotNull();
		Assertions.assertThat(((Bundle)result).getEntryFirstRep().getResource()).isInstanceOf(MessageHeader.class);
		MessageHeader messageHeader = (MessageHeader) ((Bundle)result).getEntryFirstRep().getResource();
		Assertions.assertThat(messageHeader.getResponse().getCode()).isEqualTo(MessageHeader.ResponseType.OK);
	}
	
	private void assertFailureResponse(IBaseBundle result) {
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).isInstanceOf(Bundle.class);
		Assertions.assertThat(((Bundle)result).getEntryFirstRep()).isNotNull();
		Assertions.assertThat(((Bundle)result).getEntryFirstRep().getResource()).isInstanceOf(MessageHeader.class);
		MessageHeader messageHeader = (MessageHeader) ((Bundle)result).getEntryFirstRep().getResource();
		Assertions.assertThat(messageHeader.getResponse().getCode()).isEqualTo(MessageHeader.ResponseType.FATALERROR);
	}
	
	private void assertExistsList(IBaseBundle request, IBaseBundle response) {
		MessageHeader messageHeader = ProcessMessageService.getMessageHeader((Bundle) request);		
		String patientInternalId = messageHeader.getFocusFirstRep().getReferenceElement().getIdPart();
		Identifier identifier = PatientDataReceiptService.getSourceMessageHeaderIdentifier(messageHeader);
		String searchUrl = String.format("List?subject=Patient/%s&code=%s|%s", 
				patientInternalId, PatientDataReceiptService.PDR_CODE.getSystem(), PatientDataReceiptService.PDR_CODE.getCode());		
		if(identifier != null) {
			searchUrl += String.format("&identifier=%s|%s", 
					identifier.getSystem(), identifier.getValue());
		}
		Bundle bundle = testClient.search()
			.byUrl(searchUrl)
			//assumes sequential commits, gets latest
			.sort(new SortSpec().setOrder(SortOrderEnum.DESC).setParamName("date"))
			.returnBundle(Bundle.class).execute();	
				
		Assertions.assertThat(bundle.getEntryFirstRep()).isNotNull();		
		Assertions.assertThat(bundle.getEntryFirstRep().getResource()).isInstanceOf(ListResource.class);
		ListResource pdrList = (ListResource) bundle.getEntryFirstRep().getResource();				
		if(identifier != null) {
			Assertions.assertThat(pdrList.getIdentifierFirstRep().getSystem()).isEqualTo(identifier.getSystem());
			Assertions.assertThat(pdrList.getIdentifierFirstRep().getValue()).isEqualTo(identifier.getValue());
		}
		Assertions.assertThat(pdrList.getStatus()).isEqualTo(ListResource.ListStatus.CURRENT);
		Assertions.assertThat(pdrList.getMode()).isEqualTo(ListResource.ListMode.SNAPSHOT);
		Assertions.assertThat(pdrList.getCode().getCodingFirstRep().getSystem()).isEqualTo(PatientDataReceiptService.PDR_CODE.getSystem());
		Assertions.assertThat(pdrList.getCode().getCodingFirstRep().getCode()).isEqualTo(PatientDataReceiptService.PDR_CODE.getCode());
		Assertions.assertThat(pdrList.getSubject().getReferenceElement().getIdPart()).isEqualTo(patientInternalId);
		Assertions.assertThat(pdrList.getDateElement().getValueAsString()).isNotEmpty();
		long listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> item.getReferenceElement().getResourceType().equals("Bundle"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(1);
		listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> !item.getReferenceElement().getResourceType().equals("Provenance"))
				.filter(item -> !item.getReferenceElement().getResourceType().equals("Bundle"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(((Bundle) request).getEntry().size() - 1);
		listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> item.getReferenceElement().getResourceType().equals("Provenance"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(((Bundle) request).getEntry().size() - 1);
	}
}
