package org.mitre.healthmanager.lib.pdr.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.StructureMap;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.gargoylesoftware.htmlunit.util.MimeType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@ExtendWith(MockitoExtension.class)
class StructureMapTransformerTest {	

	@Mock
	DaoRegistry myDaoRegistry;

	@Mock
	IFhirResourceDao<StructureMap> structureMapDAO;
	
	@InjectMocks
	StructureMapTransformer structureMapTransformer;

	private static final FhirContext fhirContextforR4 = FhirContext.forR4();
	
	@Test
	void testTransformConditionDstu2ToR4() throws IOException {
		
		
		StructureMap structureMap = fhirContextforR4.newJsonParser().parseResource(StructureMap.class, this.getClass().getResourceAsStream("StructureMap_PregnancyObservation.json"));

		when(structureMapDAO.read(any(IdType.class))).thenReturn(structureMap);	
		when(myDaoRegistry.getResourceDao(StructureMap.class)).thenReturn(structureMapDAO);
		
		DaoMethodOutcome resp = new DaoMethodOutcome();
		resp.setCreated(true);
		resp.setId(new IdType("HKCategoryTypeIdentifierPregnancy"));
		
		//when(patientDAO.create(structureMap)).thenReturn(resp);	
		
		//doReturn(structureMap).when(patientDAO).read(new IdType("HKCategoryTypeIdentifierPregnancy")); 
		
		//IFhirResourceDao<StructureMap> patientDAO = myDaoRegistry.getResourceDao(StructureMap.class);
		//DaoMethodOutcome resp = patientDAO.create(structueMap);
		//Assertions.assertEquals(true, resp.getCreated());

		//byte[] bytes = this.getClass().getResourceAsStream("BundleMessage_AppleHealthKit_pregnancy.json").readAllBytes();
		
		Bundle testMessage = fhirContextforR4.newJsonParser().parseResource(
				Bundle.class, this.getClass().getResourceAsStream("BundleMessage_AppleHealthKit_pregnancy.json")
        );

		Bundle.BundleEntryComponent bundleEntry =  testMessage.getEntry().get(1);
		Resource bundleResourceEntry = bundleEntry.getResource();
		((Binary)bundleResourceEntry).setContentType(MimeType.APPLICATION_JSON);
		((Binary)bundleResourceEntry).setContentType(MimeType.APPLICATION_JSON);

		// Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
		// entry.setResource(binary);

		List<Bundle.BundleEntryComponent> resultList = structureMapTransformer.transform(bundleEntry, "user-2");
		
		for (Bundle.BundleEntryComponent result : resultList) {
			Assertions.assertNotNull(result);
		}
	}
}
