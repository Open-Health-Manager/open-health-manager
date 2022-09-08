package org.mitre.healthmanager.lib;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class TestAuthorizationFilter extends GenericFilterBean {
	private String patientFhirId;
	private AuthMode mode = AuthMode.ADMIN;
	
	public enum AuthMode {
	    ADMIN,
	    USER; 
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if(mode.equals(AuthMode.ADMIN)) {
			mockAdminUser();			
		} else {
			mockPatientUser(patientFhirId);
			// set back to admin
			mode = AuthMode.ADMIN;	
		}
		
	    chain.doFilter(request, response);		
	}
	
	public void doMockUserOnce(String patientFhirId) {
		this.patientFhirId = patientFhirId; 
		mode = AuthMode.USER;
	}
	
	/// Convenience methods to update the Spring Security Context with
	/// Authorization details for test users.
	/// Created authoriztaion details are not fully valid, but represent
	/// the information used by the Health Manager Authorization and related
	/// interceptors. Thus, tests using these utilities represent only partial
	/// security tests.
	
	private void mockAdminUser() {
        
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

    private void mockPatientUser(String patientFHIRId) {
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

    private final String patientUserTemplate = "{\"sub\":\"patientUser\",\"auth\":\"ROLE_USER\",\"patient\":\"[patientFHIRId]\"}";

    private void updateSecurityContext(Authentication authentication) {
        SecurityContextHolder.clearContext();
        SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.getContext().setAuthentication(authentication); 
    }
}
