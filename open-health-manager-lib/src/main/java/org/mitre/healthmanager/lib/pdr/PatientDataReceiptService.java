package org.mitre.healthmanager.lib.pdr;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListMode;
import org.hl7.fhir.r4.model.ListResource.ListStatus;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Provenance;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.springframework.stereotype.Service;

import com.mysql.cj.util.StringUtils;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Service
public class PatientDataReceiptService {
	public static final String PDR_EVENT = "urn:mitre:healthmanager:pdr";
	public static final Coding PDR_CODE = new Coding().setSystem("urn:mitre:healthmanager").setCode("pdr");

	public static final boolean isPDRMessage(MessageHeader header) {
		Type headerEvent = header.getEvent();
		return headerEvent instanceof UriType
				? ((UriType) headerEvent).getValueAsString().equalsIgnoreCase(PDR_EVENT)
						: false;
	}

	public static final void validatePDR(Bundle theMessage) {
		if (theMessage.getEntry().size() < 2) {
			throw new UnprocessableEntityException(
					"Patient Data Receipt must have at least one additional entry beside the MessageHeader");
		}
	}

	public static final String storePDRAsRawBundle(@NotNull Bundle theMessage, @NotNull String internalPatientId, 
			@NotNull IFhirResourceDao<Bundle> bundleDao) {
			DaoMethodOutcome outcome = bundleDao.create((Bundle)theMessage);
		Objects.requireNonNull(outcome);
		Objects.requireNonNull(outcome.getResource()); 
		Objects.requireNonNull(outcome.getResource().getIdElement());
		Objects.requireNonNull(outcome.getResource().getIdElement().getIdPart());

		return outcome.getResource().getIdElement().getIdPart();
	}

	public static final Patient getPatient(@NotNull String internalPatientId, @NotNull IFhirResourceDao<Patient> patientDao) {
		return patientDao.read(new IdType(internalPatientId));
	}

	public static final Bundle storeIndividualPDREntries(@NotNull Bundle theMessage, 
			@NotNull String patientInternalId, @NotNull TransactionProcessor myTransactionProcessor, 
			@Nullable MessageHeader theHeader, @NotNull DaoRegistry daoRegistry) {
		Objects.requireNonNull(theMessage);
		Objects.requireNonNull(patientInternalId);
		Objects.requireNonNull(myTransactionProcessor);			
		Objects.requireNonNull(daoRegistry);
		Objects.requireNonNull(theMessage.getEntry());	     	     

		// need to create a new bundle because transaction processor reorders, re-ids re-references the entries
		Bundle transaction = new Bundle();
		transaction.setType(Bundle.BundleType.TRANSACTION);
		theMessage.setType(Bundle.BundleType.TRANSACTION);		
		
		// validate entries
		for(BundleEntryComponent entry : theMessage.getEntry()) {	    	
			Objects.requireNonNull(entry.getRequest());		    	
			if (entry.getRequest().getMethod() == Bundle.HTTPVerb.DELETE) {
				throw new UnprocessableEntityException("Cannot process DELETE as a part of a PDR");
			}     
			transaction.addEntry(entry.copy());			
		}
		
		return (Bundle)myTransactionProcessor.transaction((RequestDetails)null, (IBaseBundle)transaction, true);		
	}
	
	public static final ListResource createPDRList(
			@NotNull Bundle transactionRequest, @NotNull Bundle transactionResponse, 
			@NotNull String patientInternalId, @NotNull String rawBundleId,
			@NotNull MessageHeader theHeader, @NotNull DaoRegistry daoRegistry) {
		//find existing pdr list or create new
		ListResource list = findPDRList(patientInternalId, theHeader, daoRegistry);
		if(list == null) {
			list = new ListResource();
			list.setStatus(ListStatus.CURRENT);
			list.setMode(ListMode.SNAPSHOT);
			list.getCode().getCoding()
				.add(new Coding().setSystem(PDR_CODE.getSystem()).setCode(PDR_CODE.getCode()));
			list.setSubject(new Reference(new IdType("Patient", patientInternalId)));
			list.setDate(new Date());
			
			list.getIdentifier().add(getSourceMessageHeaderIdentifier(theHeader));
			
			list.getEntry().add(new ListEntryComponent()
					.setItem(new Reference(new IdType("Bundle",rawBundleId))));
		}			
		
		// add new or update existing entries
		for (int i = 0; i < transactionRequest.getEntry().size(); i++) {
			BundleEntryComponent requestEntry = transactionRequest.getEntry().get(i);
			BundleEntryComponent responseEntry = transactionResponse.getEntry().get(i);
			ListResource.ListEntryComponent entry = null;
			IdType responseId = getTransactionResponseEntryResourceId(responseEntry);
			if(responseId != null) {
				for(ListEntryComponent existingEntry : list.getEntry()) {
					// .equals(responseId) includes history version
					if(existingEntry.getItem().getReferenceElement().equals(responseId)) {
						entry = existingEntry;
						break;
					}
				}
			}
			if(entry == null) {				
				entry = list.addEntry();			
			}
			
			entry.setDate(new Date());

			if(requestEntry.getRequest().getMethod().equals(Bundle.HTTPVerb.DELETE)){
				entry.setDeleted(true);
			}
			// add created fhir id as entry.item.reference
			if(responseId != null) {
				// configured to include history version in item.reference
				entry.setItem(new Reference(responseId));	
				Provenance provenance = createPDRProvenance(requestEntry, responseEntry, patientInternalId, theHeader, daoRegistry);
				list.addEntry().setItem(new Reference(provenance.getIdElement()));
			}							
		}

		// save list
		IFhirResourceDao<ListResource> listDao = daoRegistry.getResourceDao(ListResource.class);
		DaoMethodOutcome outcome = null;
		if(list.getId() != null && !list.getId().isEmpty()) {
			 outcome = listDao.update(list);
		} else {
			 outcome = listDao.create(list);
		}		
		try {
			outcome.getResource().getIdElement().getIdPart();
		} catch (Exception ex) {
			throw new InternalErrorException("Failed to create PDR list resource.");
		}
		
		return (ListResource) outcome.getResource();
	}
	
	public static final ListResource findPDRList(@NotNull String patientInternalId,
			@NotNull MessageHeader theHeader, @NotNull DaoRegistry daoRegistry) {
		SearchParameterMap theParams = new SearchParameterMap();
		theParams.add("patient", new ReferenceParam(new IdType("Patient", patientInternalId)));				
		theParams.add("code", new TokenParam(PDR_CODE));
		if(theHeader.getSource() != null && theHeader.getSource().getEndpoint() != null
				&& theHeader.getIdElement() != null && theHeader.getIdElement().getIdPart() != null) 
		{
			theParams.add("identifier", new TokenParam()
					.setSystem(theHeader.getSource().getEndpoint())
					.setValue(theHeader.getIdElement().getIdPart()));	
			IFhirResourceDao<ListResource> listDao = daoRegistry.getResourceDao(ListResource.class);
			theParams.setSort(new SortSpec().setOrder(SortOrderEnum.DESC).setParamName("date"));
			IBundleProvider lists = listDao.search(theParams);
			if(!lists.isEmpty()){
				return (ListResource) lists.getAllResources().get(0);
			}
		}
		
		// no message header id or no matching identifier
		return null;		
	}
	
	public static final Provenance createPDRProvenance(@NotNull BundleEntryComponent requestEntry, 
			@NotNull BundleEntryComponent responseEntry,
			@NotNull String patientInternalId,
			@NotNull MessageHeader theHeader, @NotNull DaoRegistry daoRegistry) {
		IdType responseId = getTransactionResponseEntryResourceId(responseEntry);
		if(responseId == null) {
			// cannot correlate resource ids
			return null;
		}
		
		Provenance provenance = new Provenance();		
		provenance.getTarget()
			.add(new Reference(new IdType("Patient", patientInternalId)));
		provenance.getTarget()
			.add(new Reference(responseId));
		provenance.setRecorded(new Date());
		
		IdType sourceId = getTransactionRequestEntryResourceId(requestEntry, theHeader);
		Reference source = null;
		if(sourceId == null) {
			// cannot correlate resources by source id, add contained resource			
			source = new Reference();
			source.setResource(requestEntry.getResource());
		} else {
			source = new Reference(sourceId);
			// add source fhir id as meta.source
			// entity.what.reference is not searchable with non-URLs, ie urns
			// entity.what.identifier is not searchable due to HAPI FHIR not supporting reference:identifier search qualifier
			provenance.getMeta().setSourceElement(sourceId);
		}		
		provenance.getEntity().add(new Provenance.ProvenanceEntityComponent()
				.setRole(Provenance.ProvenanceEntityRole.SOURCE)
				.setWhat(source));
		
		// save Provenance
		IFhirResourceDao<Provenance> dao = daoRegistry.getResourceDao(Provenance.class);
		DaoMethodOutcome outcome = null;
		outcome = dao.create(provenance);		
		try {
			outcome.getResource().getIdElement().getIdPart();
		} catch (Exception ex) {
			throw new InternalErrorException("Failed to create Provenance resource.");
		}
		return provenance;
	}

	public static final boolean isGUID(@Nullable String theId) {		
		try {
			UUID.fromString(theId);
			return true;
		} catch (IllegalArgumentException var3) {
			return false;
		}
	}
	
	public static final Identifier getSourceMessageHeaderIdentifier(@NotNull MessageHeader theHeader) {		
		if(theHeader.getSource() != null && !StringUtils.isNullOrEmpty(theHeader.getSource().getEndpoint())
				&& theHeader.getIdElement() != null && !StringUtils.isNullOrEmpty(theHeader.getIdElement().getIdPart())) {
			return new Identifier()
				.setSystem(theHeader.getSource().getEndpoint())
				.setValue(theHeader.getIdElement().getIdPart());	
		}
		return null;
	}
	
	public static final IdType getTransactionRequestEntryResourceId(@NotNull BundleEntryComponent requestEntry,
			@NotNull MessageHeader theHeader) {
		if (requestEntry.getResource() != null && requestEntry.getResource().getIdElement() != null
				&& !StringUtils.isNullOrEmpty(requestEntry.getResource().getIdElement().getIdPart())
				&& !StringUtils.isNullOrEmpty(theHeader.getSource().getEndpoint())) {
			IdType requestId = new IdType(theHeader.getSource().getEndpoint(),
					requestEntry.getResource().getIdElement().getResourceType(),
					requestEntry.getResource().getIdElement().getIdPart(),
					requestEntry.getResource().getIdElement().getVersionIdPart());
			return requestId;
		} else if(!StringUtils.isNullOrEmpty(requestEntry.getFullUrl())) {
			return new IdType(requestEntry.getFullUrlElement());
		}
		return null;
	}
	
	public static final IdType getTransactionResponseEntryResourceId(@NotNull BundleEntryComponent responseEntry) {
		if(responseEntry.getResponse().getLocationElement() != null) {
			return new IdType(responseEntry.getResponse().getLocationElement());
		} else if (responseEntry.getResource() != null && responseEntry.getResource().getIdElement() != null
				&& !StringUtils.isNullOrEmpty(responseEntry.getResource().getIdElement().getIdPart())) {
			return responseEntry.getResource().getIdElement();
		} else if(!StringUtils.isNullOrEmpty(responseEntry.getFullUrl())) {
			return new IdType(responseEntry.getFullUrlElement());
		}
		return null;
	}
}
