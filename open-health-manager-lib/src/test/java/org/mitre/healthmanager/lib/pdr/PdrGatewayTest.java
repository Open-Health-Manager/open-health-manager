package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
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
class PdrGatewayTest {
	@Autowired
	FhirContext ourCtx;
	
    @LocalServerPort
    private int port = 0;
	
	@BeforeEach
	private void addPatient() {
		Patient patient = new Patient();
		patient.setId(new IdType("pat1"));		
		IGenericClient testClient = ourCtx.newRestfulGenericClient("http://localhost:" + port + "/fhir/");
		MethodOutcome mo = testClient.update().resource(patient).execute();
		//IFhirResourceDao<IBaseResource> patientDao = doaRegistry.getResourceDaoOrNull("Patient");
		//MethodOutcome mo = patientDao.update(patient);
		if(mo.getId() == null) {
			throw new RuntimeException();
		}
	}

	@Test
	void testProcessMessage() {							
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_valid.json")
        );
		
		// IBaseBundle response = pdrGateway.processMessage(testMessage, "http://example.org/fhir");
		Bundle response = processMessage(testMessage);
		assertSuccessResponse(response);
	}
	
	@Test
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
	void testProcessHealthKitMessage() {
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_AppleHealthKit.json")
        );
		
		Bundle response = processMessage(testMessage);
		assertSuccessResponse(response);
	}
	
	private Bundle processMessage(Bundle testMessage) {		
		ourCtx.getRestfulClientFactory().setSocketTimeout(200 * 1000);
		IGenericClient testClient = ourCtx.newRestfulGenericClient("http://localhost:" + port + "/fhir/");
		
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
}
