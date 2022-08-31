package org.mitre.healthmanager.lib.sphr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Identifier;
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
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

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
class RequestInterceptorIT {
	@Autowired
	FhirContext ourCtx;
	
    @LocalServerPort
    private int port = 0;
    
    private IGenericClient testClient;
    
	@BeforeEach
	private void setup() {		
		if(testClient == null) {
			// ourCtx.getRestfulClientFactory().setSocketTimeout(200 * 1000);
			testClient = ourCtx.newRestfulGenericClient("http://localhost:" + port + "/fhir/");
		}
	}

	@Test
	void testDisallowPatientDelete() {		
		MethodOutcome mo = testClient.create().resource(new Patient()).execute();		
				
		Assertions.assertThatThrownBy(() -> {testClient.delete().resourceById(mo.getId()).execute();})
			.isInstanceOf(UnprocessableEntityException.class);	
	}
	
	@Test
	void testPatientUpdate() {		
		Patient patient = new Patient();
		patient.getIdentifier().add(new Identifier()
				.setSystem(RequestInterceptor.FHIR_LOGIN_SYSTEM).setValue("testidentifier"));
		MethodOutcome mo = testClient.create().resource(patient).execute();
		patient.setId(mo.getId());
				
		patient.getIdentifier().remove(0);
		testClient.update().resource(patient).execute();
		
		Patient updated = testClient.read().resource(Patient.class).withId(patient.getIdElement().getIdPart()).execute();
		Assertions.assertThat(updated.getIdentifier().size()).isEqualTo(1);
		Assertions.assertThat(updated.getIdentifier().get(0).getSystem()).isEqualTo(RequestInterceptor.FHIR_LOGIN_SYSTEM);
		Assertions.assertThat(updated.getIdentifier().get(0).getValue()).isEqualTo("testidentifier");
	}
}
