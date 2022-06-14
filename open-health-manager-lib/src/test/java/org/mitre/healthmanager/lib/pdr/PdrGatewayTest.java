package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mitre.healthmanager.TestApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ca.uhn.fhir.context.FhirContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes={TestApplication.class})
class PdrGatewayTest {
	@Autowired
	FhirContext ourCtx;
	@Autowired
	PdrGateway pdrGateway;

	@Test
	void testProcessMessage() {
		Bundle testMessage = ourCtx.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_valid.json")
        );
		String result = pdrGateway.processMessage(testMessage);
		Assertions.assertThat(result).isEqualTo("done");
	}

}
