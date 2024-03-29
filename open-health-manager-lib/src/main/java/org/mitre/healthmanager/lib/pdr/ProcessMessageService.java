	package org.mitre.healthmanager.lib.pdr;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.RuntimeSearchParam;
import ca.uhn.fhir.jpa.searchparam.extractor.BaseSearchParamExtractor;
import ca.uhn.fhir.jpa.searchparam.extractor.ISearchParamExtractor;
import ca.uhn.fhir.rest.api.RestSearchParameterTypeEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Service
public class ProcessMessageService {
	@Autowired
	private FhirContext myFhirContext;

	@Autowired
	private ISearchParamExtractor mySearchParamExtractor;

	public static final MessageHeader getMessageHeader(Bundle theMessage) {
		if (theMessage.getType() != BundleType.MESSAGE) {
			throw new UnprocessableEntityException("$process-message bundle must have type 'message'");
		}

		if (theMessage.getEntry().size() > 0) {
			Resource firstEntry = theMessage.getEntry().get(0).getResource();
			if (firstEntry instanceof MessageHeader) {
				return (MessageHeader) firstEntry;
			} else {
				throw new UnprocessableEntityException(
						"First entry of the message Bundle must be a MessageHeader instance");
			}
		} else {
			throw new UnprocessableEntityException("message Bundle must have at least a MessageHeader entry");
		}
	}
	
	public IdType identifyPatientCompartment(IBaseResource theResource) {
		RuntimeResourceDefinition resourceDef = myFhirContext.getResourceDefinition(theResource);
		List<RuntimeSearchParam> compartmentSps = getPatientCompartmentSearchParams(resourceDef);
		if (compartmentSps.isEmpty()) {
			return null;
		}
		IdType compartmentIdentity = null;
		if (resourceDef.getName().equals("Patient")) {
			compartmentIdentity = (IdType) theResource.getIdElement();
			if (StringUtils.isBlank(compartmentIdentity.getIdPart())) {
				return null;
			}
		} else {
			compartmentIdentity = compartmentSps.stream()
					.map(param -> Arrays.asList(BaseSearchParamExtractor.splitPathsR4(param.getPath())))
					.flatMap(list -> list.stream())
					.filter(StringUtils::isNotBlank)
					.map(path -> mySearchParamExtractor.getPathValueExtractor(theResource, path).get())
					.filter(t -> !t.isEmpty())
					.map(t -> (IBaseReference) t)
					.map(t -> t.getReferenceElement().getValue())
					.map(t -> new IdType(t))
					.filter(t -> StringUtils.isNotBlank(t.getIdPart()))
					.findFirst()
					.orElse(null);
		}

		return compartmentIdentity;
	}
	
	private List<RuntimeSearchParam> getPatientCompartmentSearchParams(RuntimeResourceDefinition resourceDef) {
		return resourceDef
			.getSearchParams()
			.stream()
			.filter(param -> param.getParamType() == RestSearchParameterTypeEnum.REFERENCE)
			.filter(param -> param.getProvidesMembershipInCompartments() != null && param.getProvidesMembershipInCompartments().contains("Patient"))
			.collect(Collectors.toList());
	}
}
