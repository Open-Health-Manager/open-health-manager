package org.mitre.healthmanager.lib.fhir;

import javax.servlet.ServletException;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.lib.dataMgr.AccountInterceptor;
import org.mitre.healthmanager.lib.dataMgr.AccountProvider;
import org.mitre.healthmanager.lib.sphr.RequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;

@Import(AppProperties.class)
public class OHMJpaRestfulServer extends BaseJpaRestfulServer {
	@Autowired
	@Qualifier("myPatientDaoR4")
	protected IFhirResourceDaoPatient<Patient> myPatientDao;
	@Autowired
	@Qualifier("myBundleDaoR4")
	protected IFhirResourceDao<Bundle> myBundleDao;
	@Autowired
	@Qualifier("myMessageHeaderDaoR4")
	protected IFhirResourceDao<MessageHeader> myMessageHeaderDao;
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

		registerProvider(new AccountProvider(myPatientDao, myBundleDao, myMessageHeaderDao, myTransactionProcessor, myDaoRegistry));
		registerInterceptor(new AccountInterceptor());
		registerInterceptor(new RequestInterceptor(myPatientDao, myBundleDao, myMessageHeaderDao, myTransactionProcessor, myDaoRegistry));
  }

}
