package org.mitre.healthmanager.lib.pdr.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.convertors.factory.VersionConvertorFactory_10_40;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
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
	public static final Coding PREGNANT_STATUS_CODING = new Coding().setCode("77386006").setSystem("http://snomed.info/sct");
	public static final Coding NOT_PREGNANT_STATUS_CODING = new Coding().setCode("60001007").setSystem("http://snomed.info/sct");
	public static final Coding PREGNANCY_CODING = new Coding().setCode("82810-3").setSystem("http://loinc.org");
	public static final Coding PREGNANCY_SOCIAL_HISTORY_CODING = new Coding().setCode("social-history").setSystem("http://terminology.hl7.org/CodeSystem/observation-category");
	
	private static final FhirContext fhirContextforDstu2Hl7Org = FhirContext.forDstu2Hl7Org();
	
	public List<BundleEntryComponent> transformBundleEntry(BundleEntryComponent entry, String internalPatientId) {
		return transform(entry, internalPatientId);
	}
	
	@Transformer(inputChannel = "healthKitChannel")
	List<BundleEntryComponent> transform(BundleEntryComponent entry, @Header("internalPatientId") @NotNull String internalPatientId) {
		List<BundleEntryComponent> entryList = new ArrayList<BundleEntryComponent>();
		List<Resource> resourceList = new ArrayList<Resource>();
		
		Resource resourceEntry = entry.getResource();
        if (resourceEntry instanceof Binary) {
        	resourceList = convertBinary((Binary)resourceEntry, internalPatientId);
        } else {
			resourceList.add(resourceEntry);
		}

		for (Resource resource : resourceList) {
			BundleEntryComponent newEntry = new BundleEntryComponent();
			fixReferences(resource, internalPatientId);
			newEntry.setResource(resource);
			entryList.add(newEntry);
		}
        return entryList;
	}
	
	private List<Resource> convertBinary(Binary binary, String internalPatientId) {
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
	
	private List<Resource> convertPregnancy(JsonNode node) {
		String uuid = node.get("uuid").asText();

		List<String> dateArray = new ArrayList<String>();
		dateArray.add(node.get("startDate").asText());
		dateArray.add(node.get("endDate").asText());

		List<Resource> observationList = new ArrayList<Resource>();

		int index = 0;
		for (String dateString : dateArray ) {

			if(!StringUtils.isNullOrEmpty(dateString) && !dateString.equals("4000-12-31")) {
				Observation observation = new Observation();
				
				observation.setId(new IdType("HKCategoryTypeIdentifierPregnancy", uuid + "-" + index));
				
				observation.setCode(new CodeableConcept()
					.addCoding(PREGNANCY_CODING));

				observation.addCategory(new CodeableConcept()
					.addCoding(PREGNANCY_SOCIAL_HISTORY_CODING));

				if (index == 0) {
					observation.setValue(new CodeableConcept()
						.addCoding(PREGNANT_STATUS_CODING));
				} else {
					observation.setValue(new CodeableConcept()
						.addCoding(NOT_PREGNANT_STATUS_CODING));
				}
				index++;
					
				DateTimeType dateTimeEffective = new DateTimeType(dateString);
				observation.setEffective(dateTimeEffective);

				observationList.add(observation);
			}
		}
			
		return observationList;
	}
	
	private List<Resource> convertDstu2(Binary binary) {				
		String jsonStringBase64 = binary.getDataElement().getValueAsString();
		String jsonString = new String(Base64.getDecoder().decode(jsonStringBase64));

		List<Resource> resourceList = new ArrayList<Resource>();

		org.hl7.fhir.dstu2.model.Resource input = 
				(org.hl7.fhir.dstu2.model.Resource) fhirContextforDstu2Hl7Org.newJsonParser().parseResource(jsonString);
		
		if(VersionConvertorFactory_10_40.convertsResource(input.getResourceType().name())) {
			resourceList.add(VersionConvertorFactory_10_40.convertResource(input));
			return resourceList;
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
