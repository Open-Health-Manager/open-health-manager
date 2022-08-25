package org.mitre.healthmanager.lib.pdr.data;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Resource;
import org.mitre.healthmanager.lib.pdr.PatientDataReceiptService;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;

@Service
public class RecordMatchService {
	public BundleEntryComponent recordMatch(BundleEntryComponent entry, 
			@NotNull String internalPatientId,
			@NotNull MessageHeader theHeader, @NotNull DaoRegistry daoRegistry) {
		Resource resource = entry.getResource();
		BundleEntryRequestComponent request = entry.getRequest();
		
		IdType sourceId = PatientDataReceiptService.getTransactionRequestEntryResourceId(entry, theHeader);
		IdType targetId = null;
		if(sourceId != null) {
			targetId = (IdType) findExistingTargetResourceId(internalPatientId, sourceId, daoRegistry);
			if(targetId != null) {
				// existing resource update
	    		request.setUrl(targetId.getResourceType() + '/' + targetId.getIdPart());
	    		request.setMethod(Bundle.HTTPVerb.PUT);
			}
		}
			
		if(targetId == null) {
			// new resource
			targetId = new IdType(resource.getResourceType().name(), resource.getIdElement().getIdPart());
	    	request.setUrl(targetId.getResourceType());
	    	request.setMethod(Bundle.HTTPVerb.POST);		
		}		
		
		String fullUrl;
		if (targetId.getIdPart() == null) {
			fullUrl = "";
		} else if (PatientDataReceiptService.isGUID(targetId.getIdPart())) {
			fullUrl = "urn:uuid:" + targetId.getIdPart();
		} else if (!targetId.getIdPart().equals("")) {
			fullUrl = new StringBuilder()
					.append(resource.getResourceType())
					.append('/')
					.append(targetId.getIdPart()).toString();	          
		} else {
			fullUrl = "";
		}
		entry.setFullUrl(fullUrl);	

		return entry;
	}
	
	private IIdType findExistingTargetResourceId(@NotNull String patientInternalId,
			@NotNull IdType sourceId, @NotNull DaoRegistry daoRegistry) {		
		if(sourceId.getResourceType().equals("Patient")) {
			// update own patient record
			return new IdType("Patient", patientInternalId);
		}
		
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.add("patient", new ReferenceParam(new IdType("Patient", patientInternalId)));	
		theParams.add("entity", new ReferenceParam(sourceId));	
		theParams.setSort(new SortSpec().setOrder(SortOrderEnum.DESC).setParamName("recorded"));
		
		IFhirResourceDao<Provenance> dao = daoRegistry.getResourceDao(Provenance.class);
		IBundleProvider results = dao.search(theParams);
		if(!results.isEmpty()){
			Provenance provenance = (Provenance) results.getAllResources().get(0);
			if(provenance.getTarget().size() > 1) {
				return provenance.getTarget().get(1).getReferenceElement();
			}
		}
		
		return null;
	}

}
