package org.mitre.healthmanager.lib.pdr.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.convertors.factory.VersionConvertorFactory_10_40;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationComponentComponent;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Element;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.utils.structuremap.StructureMapUtilities;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.StructureMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.util.MimeType;
import com.mysql.cj.util.StringUtils;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r5.context.BaseWorkerContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Service
public class AppleHealthKitService extends DataTransformer {
	public static final Coding PREGNANT_STATUS_CODING = new Coding().setCode("77386006").setSystem("http://snomed.info/sct");
	public static final Coding NOT_PREGNANT_STATUS_CODING = new Coding().setCode("60001007").setSystem("http://snomed.info/sct");
	public static final Coding PREGNANCY_CODING = new Coding().setCode("82810-3").setSystem("http://loinc.org");
	public static final Coding PREGNANCY_SOCIAL_HISTORY_CODING = new Coding().setCode("social-history").setSystem("http://terminology.hl7.org/CodeSystem/observation-category");

	public static final Coding OBSERVATION_VITAL_SIGNS_CODING = new Coding().setCode("vital-signs").setSystem("http://terminology.hl7.org/CodeSystem/observation-category");
	public static final Coding OBSERVATION_BLOOD_PRESSURE_CODING = new Coding().setCode("85354-9").setSystem("http://loinc.org");
	public static final Coding SYSTOLIC_BLOOD_PRESSURE_CODING = new Coding().setCode("8480-6").setSystem("http://loinc.org");
	public static final Coding DIASTOLIC_BLOOD_PRESSURE_CODING = new Coding().setCode("8462-4").setSystem("http://loinc.org");
	
	private static final FhirContext fhirContextforDstu2Hl7Org = FhirContext.forDstu2Hl7Org();

	private static final Logger log = LoggerFactory.getLogger(AppleHealthKitService.class);

	@Autowired
	private DaoRegistry myDaoRegistry;

	@Autowired
	private SimpleWorkerContext context;

	@Autowired
	private StructureMapUtilities scu;
	
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
				//return transformStructureMap(node);
			}
			
			if(sampleType.equals("HKCorrelationTypeIdentifierBloodPressure")){
				return convertBloodPressure(node);
			}

		} else if(binary.getContentType().equals(Constants.CT_FHIR_JSON_NEW)){
			return convertDstu2(binary);
		}
		throw new UnprocessableEntityException("Unprocessable binary resource content type.");	
	}
	
	private List<Resource> convertBloodPressure(JsonNode node) {
		List<Resource> observationList = new ArrayList<Resource>();
		
		String uuid = node.get("uuid").asText();
		String effectiveDateString = node.get("effectiveDate").asText();

		String bloodPressureSystolic = node.get("systolicValue").asText();
		String bloodPressureDiastolic = node.get("diastolicValue").asText();

		Observation observation = new Observation();
		observation.setId(new IdType("HKCorrelationTypeIdentifierBloodPressure", uuid));

		observation.setStatus(Observation.ObservationStatus.UNKNOWN);

		observation.addCategory(new CodeableConcept().addCoding(OBSERVATION_VITAL_SIGNS_CODING));

		observation.setCode(new CodeableConcept().addCoding(OBSERVATION_BLOOD_PRESSURE_CODING));	

		DateTimeType dateTimeEffective = new DateTimeType(effectiveDateString);
		observation.setEffective(dateTimeEffective);

		observation.addComponent(createBloodPressureComponent(SYSTOLIC_BLOOD_PRESSURE_CODING, bloodPressureSystolic));
		observation.addComponent(createBloodPressureComponent(DIASTOLIC_BLOOD_PRESSURE_CODING, bloodPressureDiastolic));

		observationList.add(observation);
		return observationList;
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
		
		if(observationList.isEmpty()) {
			// bad data - sample entry should at least include startDate
			throw new UnprocessableEntityException("Unprocessable sample type: HKCategoryTypeIdentifierPregnancy");		
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
	
	private ObservationComponentComponent createBloodPressureComponent(Coding coding, String value) {
		ObservationComponentComponent component = new ObservationComponentComponent()
				.setCode(new CodeableConcept().addCoding(coding));
		if (!StringUtils.isNullOrEmpty(value)) {
			Quantity quantity = new Quantity();
			
			quantity.setValue(BigDecimal.valueOf(Double.parseDouble(value)));
			quantity.setUnit("mmHg");
			quantity.setCode("mm[Hg]");
			quantity.setSystem("http://unitsofmeasure.org");

			component.setValue(quantity);
		}  else {
			CodeableConcept dataAbsent = new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/ValueSet/data-absent-reason").setCode("unknown"));
			component.setDataAbsentReason(dataAbsent);
		}
		
		return component;
	}

	private List<Resource> transformStructureMap(JsonNode node) throws FHIRException, IOException {

		String structureMapID = node.get("structureMapID").asText();

		IFhirResourceDao<StructureMap> structureMapDAO = myDaoRegistry.getResourceDao(StructureMap.class);
		
		StructureMap structureMapFHIR = null;
    	try {
    		structureMapFHIR = structureMapDAO.read(new IdType(structureMapID));
    	} catch(ResourceNotFoundException rnfe) {    		
			throw new ResourceNotFoundException("Structure Map resource does not exist.");
		}

		//StructureMapUtilities scu = new StructureMapUtilities((IWorkerContext) context); // Not sure if this is correct, they set up the Structure Map Utilities differently- tried autowiring above
		Element src = Manager.parseSingle((IWorkerContext) context, new ByteArrayInputStream(node.binaryValue()), FhirFormat.JSON);
		Element resource = getTargetResourceFromStructureMap(structureMapFHIR, (IWorkerContext)context);

		scu.transform(null, src, structureMapFHIR, resource);
		resource.populatePaths(null); // Need to see if this is necessary, probably need to bring in manually if so

		List<Resource> resourceList = new ArrayList<Resource>();

		org.hl7.fhir.dstu2.model.Resource newResource = 
		(org.hl7.fhir.dstu2.model.Resource) fhirContextforDstu2Hl7Org.newJsonParser().parseResource(resource.toString());

		if(VersionConvertorFactory_10_40.convertsResource(newResource.getResourceType().name())) {
			resourceList.add(VersionConvertorFactory_10_40.convertResource(newResource));
			return resourceList;
		}
		throw new UnprocessableEntityException("Unprocessable binary resource fhir type: " + newResource.getResourceType().name());

	}

	private Element getTargetResourceFromStructureMap(StructureMap map, IWorkerContext context) {
		String targetTypeUrl = null;
		for (StructureMap.StructureMapStructureComponent component : map.getStructure()) {
		  if (component.getMode() == StructureMap.StructureMapModelMode.TARGET) {
			targetTypeUrl = component.getUrl();
			break;
		  }
		}
	
		if (targetTypeUrl == null) throw new FHIRException("Unable to determine resource URL for target type");
	
		StructureDefinition structureDefinition = null;
		for (StructureDefinition sd : this.context.fetchResourcesByType(StructureDefinition.class)) { // Looking at the code, it seems like something of this type should be calling this: <T extends Resource> List<T>
		  if (sd.getUrl().equalsIgnoreCase(targetTypeUrl)) {
			structureDefinition = sd;
			break;
		  }
		}
	
		if (structureDefinition == null) throw new FHIRException("Unable to find StructureDefinition for target type ('" + targetTypeUrl + "')");
	
		return Manager.build(context, structureDefinition); // Not sure what the getContext function is, need to look into this more as well
	}
}
