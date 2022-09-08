package org.mitre.healthmanager.lib.pdr.data;

import java.util.Base64;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.convertors.factory.VersionConvertorFactory_10_40;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.util.MimeType;
import com.mysql.cj.util.StringUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Service
public class AppleHealthKitService extends DataTransformer {
	public static final Coding PREGNANCY_CODING = new Coding().setCode("77386006").setSystem("http://snomed.info/sct");
	
	private static final FhirContext fhirContextforDstu2Hl7Org = FhirContext.forDstu2Hl7Org();
	
	@Transformer(inputChannel = "healthKitChannel")
	public BundleEntryComponent transform(BundleEntryComponent entry, @Header("internalPatientId") @NotNull String internalPatientId) {
		Resource resource = entry.getResource();
        if (resource instanceof Binary) {
        	resource = convertBinary((Binary)resource, internalPatientId);
        }
        fixReferences(resource, internalPatientId);
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
				return convertPregnancy(node);
			}
		} else if(binary.getContentType().equals(Constants.CT_FHIR_JSON_NEW)){
			return convertDstu2(binary);
		}
		throw new UnprocessableEntityException("Unprocessable binary resource content type.");	
	}
	
	private Condition convertPregnancy(JsonNode node) {
		String uuid = node.get("uuid").asText();
		String startDateString = node.get("startDate").asText();
		String endDateString = node.get("endDate").asText();
		
		Condition condition = new Condition();
		condition.setId(new IdType("HKCategoryTypeIdentifierPregnancy", uuid));
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
	
	private Resource convertDstu2(Binary binary) {				
		String jsonStringBase64 = binary.getDataElement().getValueAsString();
		String jsonString = new String(Base64.getDecoder().decode(jsonStringBase64));
		org.hl7.fhir.dstu2.model.Resource input = 
				(org.hl7.fhir.dstu2.model.Resource) fhirContextforDstu2Hl7Org.newJsonParser().parseResource(jsonString);
		
		if(VersionConvertorFactory_10_40.convertsResource(input.getResourceType().name())) {
			return VersionConvertorFactory_10_40.convertResource(input);
		}
		throw new UnprocessableEntityException("Unprocessable binary resource fhir type: " + input.getResourceType().name());			
	}
	
	private void fixReferences(Resource resource, String internalPatientId) {
		for(Property child : resource.children()) {
			if(child.getTypeCode().startsWith("Reference")) {
				for(Base value : child.getValues()) {
					Reference reference = (Reference) value;
					if(isPatientReference(child)) {
						if(child.getName().equals("patient") || child.getName().equals("subject")) {
							reference.setReference("Patient/" + internalPatientId);
						}
						else if(reference.getReferenceElement() != null
								&& reference.getReferenceElement().getResourceType() != null
								&& reference.getReferenceElement().getResourceType().equals("Patient")) {
							// clear performer, asserter, etc
							reference.setReference(null);
						}							
					}
				}
			}
		}
				
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
        }
	}
	
	private boolean isPatientReference(Property child) {
		if(!child.getTypeCode().startsWith("Reference(")) {
			return false;
		}
		
		if(child.getTypeCode().equals("Reference(Patient)")) {
			return true;
		} else if(child.getTypeCode().contains("(Patient|")
				|| child.getTypeCode().contains("|Patient|")
				|| child.getTypeCode().contains("|Patient)")) {
			return true;			
		}
		
		return false;
	}
}
