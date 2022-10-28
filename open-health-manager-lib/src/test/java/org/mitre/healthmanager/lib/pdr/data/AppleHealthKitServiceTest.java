package org.mitre.healthmanager.lib.pdr.data;

import java.io.IOException;
import java.util.Base64;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.gargoylesoftware.htmlunit.util.MimeType;

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
		
		Bundle.BundleEntryComponent result = ahk.transformBundleEntry(entry, "user-2");
		
		Assertions.assertNotNull(result);
		Assertions.assertEquals(result.getResource().getResourceType().name(), "Condition");
		Condition condition = (Condition) result.getResource();
		Assertions.assertEquals(condition.getSubject().getReferenceElement().getIdPart(), "user-2");
		Assertions.assertNull(condition.getAsserter().getReferenceElement().getValue());
	}
	
	@Test
	void testTransformBloodPressureToR4() throws IOException {
		byte[] bytes = this.getClass().getResourceAsStream("HKCorrelationTypeIdentifierBloodPressure.json").readAllBytes();
		
		Binary binary = new Binary();
		binary.setContentType(MimeType.APPLICATION_JSON);
		binary.setContentAsBase64(Base64.getEncoder().encodeToString(bytes));
		
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		entry.setResource(binary);
		
		Bundle.BundleEntryComponent result = ahk.transformBundleEntry(entry, "user-2");
		
		Assertions.assertNotNull(result);
		Assertions.assertEquals(result.getResource().getResourceType().name(), "Observation");
		Observation observation = (Observation) result.getResource();
		Assertions.assertEquals(observation.getSubject().getReferenceElement().getIdPart(), "user-2");
	}
}
