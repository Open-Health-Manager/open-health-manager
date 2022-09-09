package org.mitre.healthmanager.lib.sphr;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.mitre.healthmanager.lib.pdr.PdrGateway;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.jpa.dao.r4.FhirSystemDaoR4;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class ProcessMessage extends FhirSystemDaoR4 {
	@Autowired
	PdrGateway pdrGateway;
	
	public IBaseBundle processMessage(RequestDetails theRequestDetails, IBaseBundle theMessage) {
		return pdrGateway.processMessage(theMessage, theRequestDetails.getFhirServerBase());
	}
}
