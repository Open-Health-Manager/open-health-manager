package org.mitre.healthmanager.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebConfigurerTestController {

    @GetMapping("/api/test-cors")
    public void testCorsOnApiPath() {}

    @GetMapping("/test/test-cors")
    public void testCorsOnOtherPath() {}
    
    @GetMapping("/api/admin/test-cors")
    public void testCorsOnApiAdminPath() {}
    
    @GetMapping("/fhir/test-cors")
    public void testCorsOnFhirPath() {}
}
