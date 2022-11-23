package org.mitre.healthmanager.lib.pdr.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.convertors.factory.VersionConvertorFactory_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.r5.model.StructureMap;
import org.hl7.fhir.r5.model.StructureMap.StructureMapStructureComponent;
import org.hl7.fhir.r5.utils.structuremap.StructureMapUtilities;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.util.MimeType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class StructureMapTransformer extends DataTransformer {
	@Autowired
	private FhirContext fhirContext;

	@Autowired
	private DaoRegistry myDaoRegistry;

	@Autowired
	SimpleWorkerContext context;

	private static final FhirContext fhirContextforR5 = FhirContext.forR5();

	@Override
	List<BundleEntryComponent> transform(BundleEntryComponent entry, @NotNull String internalPatientId) {
		List<BundleEntryComponent> entryList = new ArrayList<BundleEntryComponent>();
		List<Resource> resourceList = new ArrayList<Resource>();

		Resource resourceEntry = entry.getResource();
		if (resourceEntry instanceof Binary) {
			try {
				resourceList = convertBinary((Binary) resourceEntry, internalPatientId);
			} catch (FHIRException | IOException e) {
				throw new UnprocessableEntityException("Unprocessable binary resource content type.");
			}
		} else {
			throw new UnprocessableEntityException("Unprocessable binary resource content type.");
		}

		for (Resource resource : resourceList) {
			BundleEntryComponent newEntry = new BundleEntryComponent();
			newEntry.setResource(resource);
			entryList.add(newEntry);
		}
		return entryList;
	}

	private List<Resource> convertBinary(Binary binary, String internalPatientId) throws FHIRException, IOException {
		if (binary.getContentType().equals(MimeType.APPLICATION_JSON)) {
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
			return transformStructureMap(node, sampleType);
		}
		throw new UnprocessableEntityException("Unprocessable binary resource content type.");
	}

	private List<Resource> transformStructureMap(JsonNode node, String sampleType) throws FHIRException, IOException {
		SimpleWorkerContext simpleWorkerContext = context; //new SimpleWorkerContext();
		
		String structureMapID = sampleType;		
		IFhirResourceDao<org.hl7.fhir.r4.model.StructureMap> structureMapDAO = myDaoRegistry
				.getResourceDao(org.hl7.fhir.r4.model.StructureMap.class);

		StructureMap structureMap = null;
		try {
			org.hl7.fhir.r4.model.StructureMap structureMapR4 = structureMapDAO.read(new IdType(structureMapID));
			structureMap = (StructureMap) VersionConvertorFactory_40_50.convertResource(structureMapR4);
		} catch (ResourceNotFoundException rnfe) {
			throw new ResourceNotFoundException("Structure Map resource does not exist.");
		}

		StructureMapUtilities scu = new StructureMapUtilities(simpleWorkerContext); // Not sure if this is correct, they set up the
																		// Structure Map Utilities differently- tried
																		// autowiring above
		org.hl7.fhir.r5.elementmodel.Element src = Manager.parseSingle(simpleWorkerContext,
				new ByteArrayInputStream(node.binaryValue()), FhirFormat.JSON);
		org.hl7.fhir.r5.elementmodel.Element resource = getTargetResourceFromStructureMap(structureMap,
				(IWorkerContext) simpleWorkerContext);

		scu.transform(null, src, structureMap, resource);

		List<Resource> resourceList = new ArrayList<Resource>();

		String resourceR5 = fhirContextforR5.newJsonParser().encodeResourceToString((IBaseResource) resource);
		Resource newResource = (Resource) fhirContext.newJsonParser().parseResource(resourceR5); 		

		resourceList.add(newResource);
		return resourceList;
	}

	private org.hl7.fhir.r5.elementmodel.Element getTargetResourceFromStructureMap(StructureMap map,
			IWorkerContext context) {
		String targetTypeUrl = null;
		for (StructureMapStructureComponent component : map.getStructure()) {
			if (component.getMode() == StructureMap.StructureMapModelMode.TARGET) {
				targetTypeUrl = component.getUrl();
				break;
			}
		}

		if (targetTypeUrl == null)
			throw new FHIRException("Unable to determine resource URL for target type " + targetTypeUrl);

		StructureDefinition structureDefinition = context.fetchResource(StructureDefinition.class,
				targetTypeUrl);

		if (structureDefinition == null)
			throw new FHIRException("Unable to determine StructureDefinition for target type " + targetTypeUrl);

		return Manager.build(context, structureDefinition);
	}
}
