package org.mitre.healthmanager.lib;

import java.util.Base64;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/// Convenience methods to update the Spring Security Context with
/// Authorization details for test users.
/// Created authoriztaion details are not fully valid, but represent
/// the information used by the Health Manager Authorization and related
/// interceptors. Thus, tests using these utilities represent only partial
/// security tests.
public class AuthorizationUtils {

    public static void mockAdminUser() {
        
        /*
            {
                "alg": "HS512"
            }
            {
                "sub": "admin",
                "auth": "ROLE_ADMIN,ROLE_USER",
                "exp": 1657306962
            }
            { ... signature ... }
        */
        String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGgiOiJST0xFX0FETUlOLFJPTEVfVVNFUiIsImV4cCI6MTY1NzMwNjk2Mn0.SgmWPeS3sbqLXTr1I8zlX26ZekwyHCr2x67gzhBaFUjaoc1d5ryP3nu_EPaJQoBkIwB9ZOO1LOAPEoz6z8y1Vg";
        
        updateSecurityContext(new UsernamePasswordAuthenticationToken("admin", token));
        
    }

    public static void mockPatientUser(String patientFHIRId) {
        /*
            {
                "alg": "HS512"
            }
            {
                "sub": "patientUser",
                "auth": "ROLE_USER",
                "patient": "[patientFHIRId]"
            }
            { ... invalid signature ... }
        */

        String patientClaims = patientUserTemplate.replace("[patientFHIRId]", patientFHIRId);
        String encodedClaims = new String(Base64.getUrlEncoder().encodeToString(patientClaims.getBytes()));
        String token = "eyJhbGciOiJIUzUxMiJ9." + encodedClaims + ".SgmWPeS3sbqLXTr1I8zlX26ZekwyHCr2x67gzhBaFUjaoc1d5ryP3nu_EPaJQoBkIwB9ZOO1LOAPEoz6z8y1Vg";
        
        updateSecurityContext(new UsernamePasswordAuthenticationToken("patientUser", token));
        
    }

    private static final String patientUserTemplate = "{\"sub\":\"patientUser\",\"auth\":\"ROLE_USER\",\"patient\":\"[patientFHIRId]\"}";

    private static void updateSecurityContext(Authentication authentication) {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.getContext().setAuthentication(authentication); 
    }
}
