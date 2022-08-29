package org.mitre.healthmanager.lib.fhir;

import javax.servlet.ServletException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.lib.sphr.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.interceptor.ExceptionHandlingInterceptor;

@Import(AppProperties.class)
public class OHMJpaRestfulServer extends BaseJpaRestfulServer {
	private static final long serialVersionUID = 1L;
	
	@Autowired
	FhirContext theCtx;

	@Autowired
	@Qualifier("myPatientDaoR4")
	protected IFhirResourceDaoPatient<Patient> myPatientDao;
	@Autowired
	@Qualifier("myBundleDaoR4")
	protected IFhirResourceDao<Bundle> myBundleDao;
	@Autowired
	@Qualifier("myListDaoR4")
	protected IFhirResourceDao<ListResource> myListDaoR4;
	@Autowired
	private TransactionProcessor myTransactionProcessor;
	@Autowired
	private DaoRegistry myDaoRegistry;
	   
	public OHMJpaRestfulServer() {
		super();
	}

	@Override
	protected void initialize() throws ServletException {									
		super.initialize();

		theCtx.getParserOptions().getDontStripVersionsFromReferencesAtPaths().add("List.entry.item");
		
		registerInterceptor(new RequestInterceptor(myPatientDao, myBundleDao, myListDaoR4, myTransactionProcessor, myDaoRegistry));

		ExceptionHandlingInterceptor interceptor = new ExceptionHandlingInterceptor();
		registerInterceptor(interceptor);
		// Return the stack trace to the client for the following exception types
		interceptor.setReturnStackTracesForExceptionTypes(InternalErrorException.class, NullPointerException.class);

	}

}
