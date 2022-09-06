package org.mitre.healthmanager;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class TestUtils {
    
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
        
        Authentication authentication = new UsernamePasswordAuthenticationToken("test", token); // mock(Authentication.class);
        //SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
        SecurityContextHolder.clearContext();
        SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.getContext().setAuthentication(authentication); 
        //when(securityContext.getAuthentication()).thenReturn(authentication);
        //SecurityContextHolder.setContext(securityContext);
        //when(SecurityContextHolder.getContext().getAuthentication().getCredentials()).thenReturn(token);
        Authentication theAuth = SecurityContextHolder.getContext().getAuthentication();
        String test = "test";

    }


}
