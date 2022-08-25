package org.mitre.healthmanager.lib.sphr;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.mitre.healthmanager.lib.pdr.PatientDataReceiptService;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;

@Interceptor
public final class RequestInterceptor {
	private final IFhirResourceDaoPatient<Patient> myPatientDaoR4;
	private final IFhirResourceDao<Bundle> myBundleDaoR4;
	private final IFhirResourceDao<ListResource> myListDaoR4;
	private final TransactionProcessor myTransactionProcessor;
	private final DaoRegistry myDaoRegistry;

	public RequestInterceptor(IFhirResourceDaoPatient myPatientDaoR4, IFhirResourceDao myBundleDaoR4,
			IFhirResourceDao<ListResource> myListDaoR4, TransactionProcessor myTransactionProcessor,
			DaoRegistry myDaoRegistry) {
		this.myPatientDaoR4 = myPatientDaoR4;
		this.myBundleDaoR4 = myBundleDaoR4;
		this.myListDaoR4 = myListDaoR4;
		this.myTransactionProcessor = myTransactionProcessor;
		this.myDaoRegistry = myDaoRegistry;				
	}
	
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
	public final boolean incomingRequestPostProcessed(@NotNull RequestDetails requestDetails, 
			@NotNull ServletRequestDetails serveletRequestDetails, 
			@NotNull HttpServletRequest theRequest, @NotNull HttpServletResponse theResponse) {
		Objects.requireNonNull(requestDetails, "requestDetails");
		Objects.requireNonNull(serveletRequestDetails, "serveletRequestDetails");
		Objects.requireNonNull(theRequest, "theRequest");
		Objects.requireNonNull(theResponse, "theResponse");
		
		// PDR delete
		if (requestDetails.getResourceName().equals("List") && requestDetails.getRequestType().equals(RequestTypeEnum.DELETE)) {
            // check that this is actually a PDR
            ListResource targetList = myListDaoR4.read(requestDetails.getId());
            if(targetList.getCode().getCodingFirstRep() != null &&
            		targetList.getCode().getCodingFirstRep().equalsShallow(PatientDataReceiptService.pdrCode)) {
                // delete PDR logic

                Bundle updateTxBundle = new Bundle();
                updateTxBundle.setType(Bundle.BundleType.TRANSACTION);
                
                // add entry to delete the list
                updateTxBundle.addEntry(new Bundle.BundleEntryComponent()
                    .setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.DELETE)
                        .setUrl("List/" + targetList.getIdElement().getIdPart())));
                
                for(ListEntryComponent entry : targetList.getEntry()) {
                	// disallow patient resource delete for now
                	if(entry.getItem() != null && !entry.getDeleted() 
                			&& entry.getItem().getReferenceElement() != null
                			&& !entry.getItem().getReferenceElement().getResourceType().equals("Patient"))
                    updateTxBundle.addEntry(new Bundle.BundleEntryComponent()
                    	.setRequest(new Bundle.BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.DELETE)
                        .setUrl(entry.getItem().getReferenceElement().getValueAsString()))); // this is version specific delete
                }
                
                // process the transaction
                myTransactionProcessor.transaction(null, updateTxBundle, false);

                // setup the response
                //todo: return a operation outcome like standard HAPI FHIR
                theResponse.setStatus(204); // no content returned, but successful
                
                return false;
            }            	
		}
		
		return true;
	}

}
