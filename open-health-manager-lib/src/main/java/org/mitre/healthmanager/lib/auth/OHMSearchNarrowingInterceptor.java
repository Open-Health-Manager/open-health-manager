package org.mitre.healthmanager.lib.auth;

import static org.mitre.healthmanager.lib.auth.AuthFetcher.getAuthorization;
import static org.mitre.healthmanager.lib.auth.AuthFetcher.parseAuthToken;

import java.util.Map;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizedList;
import ca.uhn.fhir.rest.server.interceptor.auth.SearchNarrowingInterceptor;

public class OHMSearchNarrowingInterceptor extends SearchNarrowingInterceptor {
    /**
    * This method must be overridden to provide the list of compartments
    * and/or resources that the current user should have access to
    */
    @Override
    protected AuthorizedList buildAuthorizedList(RequestDetails theRequestDetails) {
      
        String token = getAuthorization();
        Map<String, Object> claimsObject = parseAuthToken(token).getClaims();

        Object patientId = claimsObject.get("patient");
        if ((patientId != null) && (patientId instanceof String) && (!((String) patientId).isBlank())){
            // narrow searches down to the patient's compartment
            return new AuthorizedList()
                .addCompartment("Patient/" + patientId);
        } else {
            // no restrictions
            return new AuthorizedList();
        }
    }
}
