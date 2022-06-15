package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes={TestApplication.class})
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
		"spring.main.allow-bean-definition-overriding=true" })
class PdrGatewayTest {
	@Autowired
	FhirContext ourCtx;
	@Autowired
	PdrGateway pdrGateway;

	@Test
	void testProcessMessage() {
		org.hl7.fhir.r4.model.MessageHeader a;
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_valid.json")
        );
		IBaseBundle result = pdrGateway.processMessage(testMessage, "http://example.org/fhir");
		Assertions.assertThat(result).isNotNull();
	}
}
