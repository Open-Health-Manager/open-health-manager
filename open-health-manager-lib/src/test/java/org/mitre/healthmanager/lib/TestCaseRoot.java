package org.mitre.healthmanager.lib;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.context.ContextConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@ContextConfiguration(classes={org.mitre.healthmanager.lib.TestCaseRoot.TestAuthConfig.class})
public abstract class TestCaseRoot {

    private static final String fhirBaseTemplate = "http://localhost:$port/fhir/";

    protected FhirContext ourCtx = FhirContext.forR4();

    protected IGenericClient getClient(int port) {
        String fhirBase = fhirBaseTemplate.replace("$port", Integer.toString(port));
        return ourCtx.newRestfulGenericClient(fhirBase);
    }

    private Bundle getBundleFromLocation(String theLocation) throws IOException {
        InputStream inputStream;
        if (theLocation.startsWith(File.separator)) {
            inputStream = new FileInputStream(theLocation);
        }
        else {
            inputStream = new DefaultResourceLoader().getResource(theLocation).getInputStream();
        }
        
        String bundleJSON = IOUtils.toString(inputStream, com.google.common.base.Charsets.UTF_8);
        
        return ourCtx.newJsonParser().parseResource(Bundle.class, bundleJSON);
    }

    protected Bundle getBundle(String theLocation) {
        Bundle theBundle = null;
        try {
            theBundle = getBundleFromLocation(theLocation);
        } catch (IOException e) {
            fail("no data at location " + theLocation);
        }
        return theBundle;
    }

    protected Bundle submitTransactionAsAdmin(Bundle txBundle, IGenericClient client) {
        return submitTransaction(txBundle, client);
    }

    protected Bundle submitTransaction(Bundle txBundle, IGenericClient client) {
        try {
            return client.transaction().withBundle(txBundle).execute();
        } catch (Exception e) {
            fail("bundle processing failed: " + e.getMessage());
            return null;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    protected static class TestAuthConfig {
    	// only works in sequential mode, will fail if concurrent tests
		public static TestAuthorizationFilter testAuthAdminFilter;
		
        @Bean
        public FilterRegistrationBean<TestAuthorizationFilter> authAdminFilter()
        {
            FilterRegistrationBean<TestAuthorizationFilter> filterBean 
            	= new FilterRegistrationBean<>();
            testAuthAdminFilter = new TestAuthorizationFilter();
            filterBean.setFilter(testAuthAdminFilter);
            filterBean.addUrlPatterns("/*");
            filterBean.setOrder(1);
            return filterBean;    
        }
    }
}
