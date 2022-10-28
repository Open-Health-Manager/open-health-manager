package org.mitre.healthmanager.lib.pdr.data;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.rest.api.Constants;

class AppleHealthKitServiceTest {	
	private AppleHealthKitService ahk = new AppleHealthKitService();
	
	@Test
	void testTransformConditionDstu2ToR4() throws IOException {
		byte[] bytes = this.getClass().getResourceAsStream("ConditionDstu2.json").readAllBytes();
		
		Binary binary = new Binary();
		binary.setContentType(Constants.CT_FHIR_JSON_NEW);
		binary.setContentAsBase64(Base64.getEncoder().encodeToString(bytes));
		
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setResource(binary);
		
		List<Bundle.BundleEntryComponent> resultList = ahk.transformBundleEntry(entry, "user-2");
		
		for (Bundle.BundleEntryComponent result : resultList) {
			Assertions.assertNotNull(result);
			Assertions.assertEquals(result.getResource().getResourceType().name(), "Condition");
			Condition condition = (Condition) result.getResource();
			Assertions.assertEquals(condition.getSubject().getReferenceElement().getIdPart(), "user-2");
			Assertions.assertNull(condition.getAsserter().getReferenceElement().getValue());
		}
	}
}
