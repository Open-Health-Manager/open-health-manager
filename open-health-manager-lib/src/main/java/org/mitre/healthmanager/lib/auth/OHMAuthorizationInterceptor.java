package org.mitre.healthmanager.lib.auth;

import static org.mitre.healthmanager.lib.auth.AuthFetcher.getAuthorization;
import static org.mitre.healthmanager.lib.auth.AuthFetcher.parseAuthToken;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleBuilder;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

public class OHMAuthorizationInterceptor extends AuthorizationInterceptor {
    
    private final Logger log = LoggerFactory.getLogger(OHMAuthorizationInterceptor.class);

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

        log.debug("building rule for request {}", theRequestDetails.getRequestPath());
        String requestPath = theRequestDetails.getRequestPath();
        
        if (requestPath.equals("metadata") || requestPath.equals("api-docs") || requestPath.equals("swagger-ui/")) {
            return metadataRule();
        }

        String token = getAuthorization();
        Map<String, Object> claimsObject = parseAuthToken(token).getClaims();

        Object patientId = claimsObject.get("patient");
        Object authorities = claimsObject.get("auth");
        log.debug("building rule for linked patient {}", patientId);
        log.debug("building rule for authorities {}", authorities);

        if ((authorities instanceof String) && ((String) authorities).contains("ROLE_ADMIN")) {
            return adminRule();
        }
        
        // If the user is a specific patient, we create a patient-specific rule chain
        if ((patientId != null) && (patientId instanceof String) && (!((String) patientId).isBlank())){
            return patientRule(patientId);
        }

        // By default, deny everything.
        return new RuleBuilder()
            .denyAll("Default deny")
            .build();
   }

    private List<IAuthRule> patientRule(Object patientId) {
        IdType userIdPatientId = new IdType("Patient", (String) patientId);
        
        IAuthRuleBuilder patientAccessRules = new RuleBuilder()
        .allow("patient compartment read").read().allResources().inCompartment("Patient", userIdPatientId).andThen()
        .allow("update patient record").write().instance(userIdPatientId).andThen()
        .deny("delete patient record").delete().instance(userIdPatientId).andThen() //disallow patient delete outside of full account deletion
        .deny("no patient resource creation").write().resourcesOfType(Patient.class).withAnyId().andThen()
        .allow("patient compartment write").write().allResources().inCompartment("Patient", userIdPatientId).andThen()
        .allow("transactions").transaction().withAnyOperation().andApplyNormalRules().andThen()
        .allow("process-message").operation().named("process-message").onServer().andAllowAllResponses().andThen()
        ;
        
        return patientAccessRules.denyAll("patient access restricted").andThen().build();
    }

    private List<IAuthRule> adminRule() {
        return new RuleBuilder()
            .allowAll("admin allow")
            .build();
    }

   private List<IAuthRule> metadataRule() {    
        // allow the metadata endpoint for everyone
        return new RuleBuilder()
            .allow("allow metadata")
            .metadata()
            .build();
   }
}
