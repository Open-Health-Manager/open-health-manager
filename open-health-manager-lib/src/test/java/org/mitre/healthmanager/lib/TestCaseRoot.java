package org.mitre.healthmanager.lib;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.core.io.DefaultResourceLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class TestCaseRoot {

    @BeforeEach
    public void cacheAndSetSecurityContextStrategy() {
        cacheModeAndMakeGlobal();
    }

    @AfterEach
    public void restoreSecurityContextStrategy() {
        restoreMode();
    }

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
        AuthorizationUtils.mockAdminUser();
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

    private SecurityContextHolderStrategy originalMode;

    public void cacheModeAndMakeGlobal() {
        cacheMode();
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
    }

    public void cacheMode() {
        originalMode = SecurityContextHolder.getContextHolderStrategy();
    }

    public void restoreMode() {
        SecurityContextHolder.setContextHolderStrategy(originalMode);
    }


}
