package org.mitre.healthmanager.lib.auth;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Organization;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;

public class OHMConsentInterceptor implements IConsentService {

    /**
     * Invoked once at the start of every request
     */
    @Override
    public ConsentOutcome startOperation(RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
        // This means that all requests should flow through the consent service
        // This has performance implications - If you know that some requests
        // don't need consent checking it is a good idea to return
        // ConsentOutcome.AUTHORIZED instead for those requests.
        return ConsentOutcome.PROCEED;
    }
 
    /**
     * Can a given resource be returned to the user?
     */
    @Override
    public ConsentOutcome canSeeResource(RequestDetails theRequestDetails, IBaseResource theResource, IConsentContextServices theContextServices) {
        // In this basic example, we will filter out lab results so that they
        // are never disclosed to the user. A real interceptor might do something
        // more nuanced.
        if (theResource instanceof Organization) {
            return ConsentOutcome.REJECT;
        }
 
       // Otherwise, allow the
       return ConsentOutcome.PROCEED;
    }
 
   
 }
