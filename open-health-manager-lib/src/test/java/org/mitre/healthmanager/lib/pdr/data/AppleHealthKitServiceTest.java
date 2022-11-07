package org.mitre.healthmanager.lib.pdr.data;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

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

	@Test
	void testConvertPregnancy() throws IOException {

		String pregnancyMessage = "{\"uuid\":\"24827DF1-BB01-4D39-A3C5-3F29A4C8FC5B\",\"value\":\"notApplicable\",\"startDate\":\"2022-09-03\",\"endDate\":\"2023-06-03\",\"sampleType\":\"HKCategoryTypeIdentifierPregnancy\"}";
		Binary pregnancyResource = new Binary().setContentType("application/json");
		pregnancyResource.setContentAsBase64(Base64.getEncoder().encodeToString(pregnancyMessage.getBytes()));
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent().setResource(pregnancyResource);
		
		List<Bundle.BundleEntryComponent> resultList = ahk.transformBundleEntry(entry, "user-2");
		Assertions.assertEquals(2, resultList.size());
		for (int i = 0; i < resultList.size(); i++) {	
			Bundle.BundleEntryComponent result = resultList.get(i);
			Assertions.assertNotNull(result);
			Assertions.assertEquals("Observation", result.getResource().getResourceType().name());
			Observation observation = (Observation) result.getResource();
			Assertions.assertEquals(observation.getSubject().getReferenceElement().getIdPart(), "user-2");
			if(i == 0) {
				Assertions.assertTrue(observation.getCode().getCodingFirstRep().equalsShallow(AppleHealthKitService.PREGNANCY_CODING));
				Assertions.assertTrue(observation.getValueCodeableConcept().getCodingFirstRep().equalsShallow(AppleHealthKitService.PREGNANT_STATUS_CODING));
				Assertions.assertTrue(observation.getEffectiveDateTimeType().getValueAsString().equals("2022-09-03"));
					
		 	} else if(i == 1) {
				Assertions.assertTrue(observation.getCode().getCodingFirstRep().equalsShallow(AppleHealthKitService.PREGNANCY_CODING));
				Assertions.assertTrue(observation.getValueCodeableConcept().getCodingFirstRep().equalsShallow(AppleHealthKitService.NOT_PREGNANT_STATUS_CODING));
				Assertions.assertTrue(observation.getEffectiveDateTimeType().getValueAsString().equals("2023-06-03"));
			
			}
		}
	}
	
	@Test
	void testConvertPregnancyNoEndDate() throws IOException {
		String pregnancyMessage = "{\"uuid\":\"34827DF1-BB01-4D39-A3C5-3F29A4C8FC5B\",\"value\":\"notApplicable\",\"startDate\":\"2022-09-03\",\"endDate\":\"4000-12-31\",\"sampleType\":\"HKCategoryTypeIdentifierPregnancy\"}";
		Binary pregnancyResource = new Binary().setContentType("application/json");
		pregnancyResource.setContentAsBase64(Base64.getEncoder().encodeToString(pregnancyMessage.getBytes()));
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent().setResource(pregnancyResource);

		List<Bundle.BundleEntryComponent> resultList = ahk.transformBundleEntry(entry, "user-2");
		Assertions.assertEquals(1, resultList.size());
		Bundle.BundleEntryComponent result = resultList.get(0);
		Assertions.assertNotNull(result);
		Assertions.assertEquals("Observation", result.getResource().getResourceType().name());
		Observation observation = (Observation) result.getResource();
		Assertions.assertEquals(observation.getSubject().getReferenceElement().getIdPart(), "user-2");
		Assertions.assertTrue(observation.getCode().getCodingFirstRep().equalsShallow(AppleHealthKitService.PREGNANCY_CODING));
		Assertions.assertTrue(observation.getValueCodeableConcept().getCodingFirstRep().equalsShallow(AppleHealthKitService.PREGNANT_STATUS_CODING));
		Assertions.assertTrue(observation.getEffectiveDateTimeType().getValueAsString().equals("2022-09-03"));
	}
	
	@Test
	void testConvertPregnancyNoDate() throws IOException {
		String pregnancyMessage = "{\"uuid\":\"34827DF1-BB01-4D39-A3C5-3F29A4C8FC5B\",\"value\":\"notApplicable\",\"startDate\":\"4000-12-31\",\"endDate\":\"4000-12-31\",\"sampleType\":\"HKCategoryTypeIdentifierPregnancy\"}";
		Binary pregnancyResource = new Binary().setContentType("application/json");
		pregnancyResource.setContentAsBase64(Base64.getEncoder().encodeToString(pregnancyMessage.getBytes()));
		Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent().setResource(pregnancyResource);

		Assertions.assertThrows(UnprocessableEntityException.class, () -> ahk.transformBundleEntry(entry, "user-2"));
	}
}
