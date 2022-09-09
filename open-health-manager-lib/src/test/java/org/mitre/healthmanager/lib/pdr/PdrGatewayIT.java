package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.TestApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.rest.api.MethodOutcome;

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
class PdrGatewayIT extends BasePdrIT {	
    @LocalServerPort
    private int port = 0;       
    
	@BeforeEach
	private void addPatient() {
		initClient(port);
		
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
	

}
