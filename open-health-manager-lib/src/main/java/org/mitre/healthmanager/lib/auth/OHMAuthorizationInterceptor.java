package org.mitre.healthmanager.lib.auth;

import java.security.Key;
import java.util.Base64;
import java.util.List;

import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;



public class OHMAuthorizationInterceptor extends AuthorizationInterceptor {
    
    private final Logger log = LoggerFactory.getLogger(OHMAuthorizationInterceptor.class);

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

        log.debug("building rule for request {}", theRequestDetails.getRequestPath());
        String requestPath = theRequestDetails.getRequestPath();
        
        // allow the metadata endpoint for everyone
        if (requestPath.equals("metadata")) {
            return new RuleBuilder()
                .allow("allow metadata")
                .metadata()
                .build();
        }
/* 
        String authHeader = theRequestDetails.getHeader("Authorization");
        if ((authHeader == null) || !authHeader.startsWith("Bearer ")) {
            // Throw an HTTP 401
            throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authorization header value");
        }
*/
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if ((authentication == null) || !(authentication.getCredentials() instanceof String)) {
            throw new AuthenticationException(Msg.code(644) + "Missing or invalid Authentication");
        }
        
        String token = (String) authentication.getCredentials();
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
        Object authorities = claimsObject.get("auth");
        log.debug("building rule for linked patient {}", patientId);
        log.debug("building rule for authorities {}", authorities);

        if ((authorities instanceof String) && ((String) authorities).contains("ROLE_ADMIN")) {
            return new RuleBuilder()
                .allowAll("admin allow")
                .build();
        }
        
        // If the user is a specific patient, we create the following rule chain:
        // Allow the user to read anything in their own patient compartment
        // Allow the user to write anything in their own patient compartment
        // If a client request doesn't pass either of the above, deny it
        if ((patientId != null) && (patientId instanceof String) && (!((String) patientId).isBlank())){
            IdType userIdPatientId = new IdType("Patient", (String) patientId);
            return new RuleBuilder()
                .allow("patient read").read().allResources().inCompartment("Patient", userIdPatientId).andThen()
                .allow("patient write").write().allResources().inCompartment("Patient", userIdPatientId).andThen()
                .denyAll("patient access restricted")
                .build();
        }

        // By default, deny everything.
        return new RuleBuilder()
            .denyAll("Default deny")
            .build();
   }
}
