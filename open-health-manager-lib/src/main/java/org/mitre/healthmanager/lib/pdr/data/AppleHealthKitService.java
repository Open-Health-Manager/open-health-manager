package org.mitre.healthmanager.lib.pdr.data;

import java.util.Base64;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.util.MimeType;
import com.mysql.cj.util.StringUtils;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Service
public class AppleHealthKitService extends DataTransformer {
	public static final Coding PREGNANCY_CODING = new Coding().setCode("77386006").setSystem("http://snomed.info/sct");
	
	@Transformer(inputChannel = "healthKitChannel")
	BundleEntryComponent transform(BundleEntryComponent entry, @Header("internalPatientId") @NotNull String internalPatientId) {
		Resource resource = entry.getResource();
        if (resource instanceof Observation) {
        	((Observation)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Observation)resource).setEncounter((Reference)null);
        } else if (resource instanceof Procedure) {
        	((Procedure)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Procedure)resource).setEncounter((Reference)null);
           ((Procedure)resource).getPerformer().clear();
        } else if (resource instanceof Condition) {
        	((Condition)resource).getSubject().setReference("Patient/" + internalPatientId);
           ((Condition)resource).setAsserter((Reference)null);
        } else if (resource instanceof AllergyIntolerance) {
        	((AllergyIntolerance)resource).getPatient().setReference("Patient/" + internalPatientId);
        } else if (resource instanceof Immunization) {
        	((Immunization)resource).getPatient().setReference("Patient/" + internalPatientId);
           ((Immunization)resource).setEncounter((Reference)null);
           ((Immunization)resource).getPerformer().clear();
        } else if (resource instanceof Binary) {
        	resource = convertBinary((Binary)resource, internalPatientId);
        }
        entry.setResource(resource);
        return entry;
	}
	
	private Resource convertBinary(Binary binary, String internalPatientId) {
		if(binary.getContentType().equals(MimeType.APPLICATION_JSON)) {
			String jsonStringBase64 = binary.getDataElement().getValueAsString();
			String jsonString = new String(Base64.getDecoder().decode(jsonStringBase64));
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = null;
			try {
				node = mapper.readTree(jsonString);
			} catch (JsonProcessingException e) {
				throw new UnprocessableEntityException("Unprocessable binary resource data.");
			}
			String sampleType = node.get("sampleType").asText();
			if(sampleType.equals("HKCategoryTypeIdentifierPregnancy")){
				return convertPregnancy(node, internalPatientId);
			}
		}
		throw new UnprocessableEntityException("Unprocessable binary resource content type.");	
	}
	
	private Condition convertPregnancy(JsonNode node, String internalPatientId) {
		String uuid = node.get("uuid").asText();
		String startDateString = node.get("startDate").asText();
		String endDateString = node.get("endDate").asText();
		
		Condition condition = new Condition();
		condition.setId(new IdType("HKCategoryTypeIdentifierPregnancy", uuid));
		condition.getSubject().setReference("Patient/" + internalPatientId);
		condition.setCode(new CodeableConcept()
				.addCoding(PREGNANCY_CODING));
		
		Period onset = new Period();
		condition.setOnset(onset);		
		if(!StringUtils.isNullOrEmpty(startDateString)) {
			DateType startDate = new DateType(startDateString);
			onset.setStart(startDate.getValue());
		}

		if(!StringUtils.isNullOrEmpty(endDateString) && !endDateString.equals("4000-12-31")) {
			DateType endDate = new DateType(endDateString);
			onset.setEnd(endDate.getValue());		
		}
			
		return condition;
	}
}
