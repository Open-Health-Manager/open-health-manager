package org.mitre.healthmanager.lib.sphr;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.lib.pdr.PatientDataReceiptService;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

@Interceptor
public final class RequestInterceptor {
	public static final String FHIR_LOGIN_SYSTEM = "urn:mitre:healthmanager:account:username";
	
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
			@NotNull ServletRequestDetails servletRequestDetails, 
			@NotNull HttpServletRequest theRequest, @NotNull HttpServletResponse theResponse) {
		Objects.requireNonNull(requestDetails, "requestDetails");
		Objects.requireNonNull(servletRequestDetails, "servletRequestDetails");
		Objects.requireNonNull(theRequest, "theRequest");
		Objects.requireNonNull(theResponse, "theResponse");
		
		// disallow patient delete
        if (requestDetails.getResourceName() != null && requestDetails.getResourceName().equals("Patient")
        		&& requestDetails.getRequestType().equals(RequestTypeEnum.DELETE)){
        	throw new UnprocessableEntityException("Direct patient deletion not supported");
        }
        
		// handle patient updates
        if (requestDetails.getResourceName() != null && requestDetails.getResourceName().equals("Patient")
        		&& requestDetails.getRequestType().equals(RequestTypeEnum.PUT)){
        	updatePatient(requestDetails);
        }
        
		// PDR delete
		if (requestDetails.getResourceName() != null && requestDetails.getResourceName().equals("List") 
				&& requestDetails.getRequestType() != null && requestDetails.getRequestType().equals(RequestTypeEnum.DELETE)) {
			return deletePdr(requestDetails, theResponse);
		}
		
		return true;
	}
	
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void reactToRequest(RequestDetails requestDetails,
    		ServletRequestDetails serveletRequestDetails,
            RestOperationTypeEnum restOperation
    ) {

        if (requestDetails.getUserData().get("BaseStorageDao.processingSubRequest") != null) {
            return;
        }
        
        if(restOperation.equals(RestOperationTypeEnum.UPDATE)) {
    		// handle patient updates
            if (requestDetails.getResourceName() != null && requestDetails.getResourceName().equals("Patient")
            		&& requestDetails.getRequestType().equals(RequestTypeEnum.PUT)){
            	updatePatient(requestDetails);
            }
        }
    }
	
	@SuppressWarnings("unchecked")
	private boolean updatePatient(RequestDetails requestDetails) {
		// check patient exists
		Patient originalPatient = null;
		try {
			originalPatient = PatientDataReceiptService.getPatient(
					requestDetails.getId().getIdPart(), 
					myPatientDaoR4);
		} catch (ResourceNotFoundException e) {
			return true;
		}
		
		// add back account identifier in case altered
		Patient patient = (Patient) requestDetails.getResource();
		if(patient !=  null) {		
			Optional<Identifier> user = originalPatient.getIdentifier().stream()
					.filter(identifier -> identifier.getSystem().equals(FHIR_LOGIN_SYSTEM)).findFirst();
			user.ifPresent(theUser -> {
				List<Identifier> newIdentifiers = patient.getIdentifier().stream()
						.filter(identifier -> !identifier.getSystem().equals(FHIR_LOGIN_SYSTEM))
						.collect(Collectors.toList());	
				newIdentifiers.add(theUser);
				patient.setIdentifier(newIdentifiers);
			});				
		}
		
		return true;
	}
	
	private boolean deletePdr(RequestDetails requestDetails, HttpServletResponse theResponse) {
		// check that this is actually a PDR
        ListResource targetList = myListDaoR4.read(requestDetails.getId());
        if(targetList.getCode().getCodingFirstRep() != null &&
        		targetList.getCode().getCodingFirstRep().equalsShallow(PatientDataReceiptService.PDR_CODE)) {
            // delete PDR logic

            Bundle updateTxBundle = new Bundle();
            updateTxBundle.setType(Bundle.BundleType.TRANSACTION);
            
            // add entry to delete the list
            updateTxBundle.addEntry(new Bundle.BundleEntryComponent()
                .setRequest(new Bundle.BundleEntryRequestComponent()
                    .setMethod(Bundle.HTTPVerb.DELETE)
                    .setUrl("List/" + targetList.getIdElement().getIdPart())));
            
            for(ListEntryComponent entry : targetList.getEntry()) {
            	// disallow patient resource delete
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
        
       return true;
	}
}
