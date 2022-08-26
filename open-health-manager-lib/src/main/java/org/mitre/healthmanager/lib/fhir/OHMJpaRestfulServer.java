package org.mitre.healthmanager.lib.fhir;

import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;

@Import(AppProperties.class)
public class OHMJpaRestfulServer extends BaseJpaRestfulServer {
	private static final long serialVersionUID = 1L;
	
	@Autowired
	FhirContext theCtx;
	@Autowired
	private PartitionSettings myPartitionSettings;
	@Autowired
	PartitionManagementProvider partitionManagementProvider;
	   
	public OHMJpaRestfulServer() {
		super();
	}

	@Override
	protected void initialize() throws ServletException {		
		//myPartitionSettings.setPartitioningEnabled(true);
		//myPartitionSettings.setUnnamedPartitionMode(true);
		//setTenantIdentificationStrategy(new OHMTenantIdentificationStrategy());
		//registerInterceptor(new RequestTenantPartitionInterceptor());
		//registerProviders(partitionManagementProvider);	
				
		theCtx.getParserOptions().getDontStripVersionsFromReferencesAtPaths().add("List.entry.item");
		super.initialize();
	}

}
