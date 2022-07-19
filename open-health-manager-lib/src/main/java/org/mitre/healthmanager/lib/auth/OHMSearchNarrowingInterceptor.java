package org.mitre.healthmanager.lib.auth;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizedList;
import ca.uhn.fhir.rest.server.interceptor.auth.SearchNarrowingInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Base64;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ca.uhn.fhir.i18n.Msg;

public class OHMSearchNarrowingInterceptor extends SearchNarrowingInterceptor {
    /**
    * This method must be overridden to provide the list of compartments
    * and/or resources that the current user should have access to
    */
    @Override
    protected AuthorizedList buildAuthorizedList(RequestDetails theRequestDetails) {
      
        /// Get from Spring auth context, but fall back to the request if needed
        String token;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            String authHeader = theRequestDetails.getHeader("Authorization");
            if (authHeader == null) {
                throw new AuthenticationException(Msg.code(644) + "Missing Authorization");
            }
            else if (!authHeader.startsWith("Bearer ")) {
                throw new AuthenticationException(Msg.code(644) + "Invalid Authorization");
            }
            else {
                token = theRequestDetails.getHeader("Authorization").substring(7);
            }
        }
        else {
            if (!(authentication.getCredentials() instanceof String)) {
                throw new AuthenticationException(Msg.code(644) + "Invalid Authorization");
            }
            else {
                token = (String) authentication.getCredentials();
            }
        }

        //String token = (String) authentication.getCredentials();
        String body = token.split("\\.")[1];
        String jsonBody = new String(Base64.getUrlDecoder().decode(body));
        JSONObject claimsObject = null;
        JSONParser parser = new JSONParser();
        try {
            Object parsedBody = parser.parse(jsonBody);
            if (parsedBody instanceof JSONObject) {
                claimsObject = (JSONObject) parsedBody;
            }
            else {
                throw new AuthenticationException(Msg.code(644) + "jwt body not a json object");
            }
        } catch (ParseException e) {
            throw new AuthenticationException(Msg.code(644) + "invalid jwt body");
        }

        Object patientId = claimsObject.get("patient");
        if ((patientId != null) && (patientId instanceof String) && (!((String) patientId).isBlank())){
            // This user will have access to only their compartment
            return new AuthorizedList()
                .addCompartment("Patient/" + patientId);
        } else {
            // no restrictions
            return new AuthorizedList();

        }

    }
}
