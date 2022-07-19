package org.mitre.healthmanager.lib.auth;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.partition.SystemRequestDetails;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRuleBuilder;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;



public class OHMAuthorizationInterceptor extends AuthorizationInterceptor {
    
    private final Logger log = LoggerFactory.getLogger(OHMAuthorizationInterceptor.class);

    private DaoRegistry myDaoRegistry;

    public OHMAuthorizationInterceptor(DaoRegistry theDaoRegistry) {
        super();
        myDaoRegistry = theDaoRegistry;
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

        log.debug("building rule for request {}", theRequestDetails.getRequestPath());
        String requestPath = theRequestDetails.getRequestPath();
        
        if (requestPath.equals("metadata")) {
            return metadataRule();
        }

        String token = getAuthorization();
        JSONObject claimsObject = parseAuthToken(token);

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
        .allow("patient read").read().allResources().inCompartment("Patient", userIdPatientId).andThen()
        .allow("patient write").write().allResources().inCompartment("Patient", userIdPatientId).andThen()
        .allow("patient everything").operation().named("$everything").onInstance(userIdPatientId).andAllowAllResponses().andThen()
        .allow("practitioner").read().resourcesOfType("Practitioner").withAnyId().andThen()
        //.allow("organization").read().resourcesOfType("Organization").withAnyId().andThen()
        .allow("location").read().resourcesOfType("Location").withAnyId().andThen()
        .allow("practitioner role").read().resourcesOfType("PractitionerRole").withAnyId().andThen()
        ;

        /// Get an explicit list of message headers and bundles representing PDRs
        patientAccessRules = buildPDRRules(userIdPatientId, patientAccessRules);
        
        return patientAccessRules.denyAll("patient access restricted").andThen().build();
    }

    private IAuthRuleBuilder buildPDRRules(IdType userIdPatientId, IAuthRuleBuilder patientAccessRules) {
        IFhirResourceDao<MessageHeader> messageHeaderDao = myDaoRegistry.getResourceDao(MessageHeader.class);
        SystemRequestDetails searchRequestDetails = SystemRequestDetails.forAllPartition();
        searchRequestDetails.addHeader("Cache-Control", "no-cache");
        IBundleProvider searchResults = 
            messageHeaderDao.search(
                new SearchParameterMap(
                    "focus", 
                    new ReferenceParam(userIdPatientId)
                ),
                searchRequestDetails
            );
        if (!searchResults.isEmpty()) {
            
            List<IIdType> pdrInstances = new ArrayList<IIdType>();
            for (IBaseResource theResource : searchResults.getAllResources()) {
                if (theResource instanceof MessageHeader) {
                    MessageHeader theHeader = (MessageHeader) theResource;
                    pdrInstances.add(new IdType("MessageHeader", theHeader.getIdElement().getIdPart()));
                    for (Reference theRef : theHeader.getFocus()) {
                        if (theRef.getReference().startsWith("Bundle")) {
                            pdrInstances.add(new IdType(theRef.getReference()));
                            break;
                        }
                    }
                }
                else {
                    throw new RuntimeException("Internal search for MessageHeaders returned something other than a message header.");
                }
            }
            patientAccessRules = patientAccessRules.allow("pdr instances (MessageHeader and Bundles)").read().instances(pdrInstances).andThen();
        }
        return patientAccessRules;
    }

    private List<IAuthRule> adminRule() {
        return new RuleBuilder()
            .allowAll("admin allow")
            .build();
    }

    private JSONObject parseAuthToken(String token) {
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
        return claimsObject;
    }

    private String getAuthorization() {
        /// Get from Spring auth context, but fall back to the request if needed
        String token;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AuthenticationException(Msg.code(644) + "Missing Authorization");
        }
        else {
            if (!(authentication.getCredentials() instanceof String)) {
                throw new AuthenticationException(Msg.code(644) + "Invalid Authorization");
            }
            else {
                token = (String) authentication.getCredentials();
            }
        }
        return token;
    }

   private List<IAuthRule> metadataRule() {
    // allow the metadata endpoint for everyone
    
        return new RuleBuilder()
            .allow("allow metadata")
            .metadata()
            .build();
   }
}