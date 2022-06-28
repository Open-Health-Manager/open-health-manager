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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;

@Interceptor
public final class RequestInterceptor {
	private final IFhirResourceDaoPatient myPatientDaoR4;
	private final IFhirResourceDao myBundleDaoR4;
	private final IFhirResourceDao myMessageHeaderDaoR4;
	private final TransactionProcessor myTransactionProcessor;
	private final DaoRegistry myDaoRegistry;

	public RequestInterceptor(IFhirResourceDaoPatient myPatientDaoR4, IFhirResourceDao myBundleDaoR4,
			IFhirResourceDao myMessageHeaderDaoR4, TransactionProcessor myTransactionProcessor,
			DaoRegistry myDaoRegistry) {
		this.myPatientDaoR4 = myPatientDaoR4;
		this.myBundleDaoR4 = myBundleDaoR4;
		this.myMessageHeaderDaoR4 = myMessageHeaderDaoR4;
		this.myTransactionProcessor = myTransactionProcessor;
		this.myDaoRegistry = myDaoRegistry;
	}

}
