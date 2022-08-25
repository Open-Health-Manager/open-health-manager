package org.mitre.healthmanager.lib.fhir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.tenant.ITenantIdentificationStrategy;
import ca.uhn.fhir.util.UrlPathTokenizer;

public class OHMTenantIdentificationStrategy implements ITenantIdentificationStrategy {

	private static final Logger log = LoggerFactory.getLogger(OHMTenantIdentificationStrategy.class);

	@Override
	public void extractTenant(UrlPathTokenizer theUrlPathTokenizer, RequestDetails theRequestDetails) {
		String tenantId = RequestPartitionId.defaultPartition().getFirstPartitionNameOrNull();
		theRequestDetails.setTenantId(tenantId);
		log.trace("Found tenant ID {}", tenantId);
	}

	@Override
	public String massageServerBaseUrl(String theFhirServerBase, RequestDetails theRequestDetails) {		
		return theFhirServerBase;
	}

}