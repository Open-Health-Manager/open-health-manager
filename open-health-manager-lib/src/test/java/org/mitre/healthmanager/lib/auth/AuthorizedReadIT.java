package org.mitre.healthmanager.lib.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.TestApplication;
import org.mitre.healthmanager.lib.TestCaseRoot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class,
    properties = {
        "spring.batch.job.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:dbr4",
        "spring.datasource.username=sa",
        "spring.datasource.password=null",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
        "hapi.fhir.enable_repository_validating_interceptor=true",
        "hapi.fhir.fhir_version=r4",
    }
)
@ContextConfiguration
public class AuthorizedReadIT extends TestCaseRoot {

    public AuthorizedReadIT() {
        IRestfulClientFactory factory = ourCtx.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        factory.setSocketTimeout(1200 * 1000);
    }

    @LocalServerPort
    private int port = 0;

    @Test
    // List of supported / validated patient compartment resources present in 
    // the corresponding transaction bundle: 
    // healthmanager/lib/auth/AuthorizedReadTests/allowUserReadInPatientCompartment/Bundle_create.json
    public void allowUserReadInPatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedReadTests/allowUserReadInPatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());        

        for (int indexEntry = 0 ; indexEntry < responseEntries.size() ; indexEntry++) {
            String[] parsedLocation = responseEntries.get(indexEntry).getResponse().getLocation().split("/");
            assertTrue(parsedLocation.length > 1);
            String resourceType = parsedLocation[0];
            String resourceId = parsedLocation[1];
            TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-allowUserReadInPatientCompartment");
            IBaseResource result = theClient.read().resource(resourceType).withId(resourceId).execute();
            assertNotNull(result);
        }

    }

    @Test
    @Disabled // transaction not currently creating a MessageHeader
    // Currently attempts to read the MessageHeader for the PDR
    // NOTE: this may be allowed in the future
    public void rejectUserReadOutsidePatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedReadTests/rejectUserReadOutsidePatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        assertEquals(createBundle.getEntry().size(), createResult.getEntry().size());

        IBaseBundle messageHeaderResults = theClient.search().forResource(MessageHeader.class).where(MessageHeader.FOCUS.hasId(new IdDt("Patient", "test-rejectUserReadOutsidePatientCompartment"))).execute();
        assertTrue(messageHeaderResults instanceof Bundle);

        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-rejectUserReadOutsidePatientCompartment");
        Bundle messageHeaderResultsBundle = (Bundle) messageHeaderResults;
        assertEquals(1, messageHeaderResultsBundle.getEntry().size());
        String resourceId = messageHeaderResultsBundle.getEntryFirstRep().getResource().getIdElement().getIdPart();
        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.read().resource(MessageHeader.class).withId(resourceId).execute(); },
            "access not restricted"
        );

    }

    @Test
    public void rejectUserReadInAnotherPatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedReadTests/rejectUserReadInAnotherPatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());        

        for (int indexEntry = 0 ; indexEntry < responseEntries.size() ; indexEntry++) {
            String[] parsedLocation = responseEntries.get(indexEntry).getResponse().getLocation().split("/");
            assertTrue(parsedLocation.length > 1);
            String resourceType = parsedLocation[0];
            String resourceId = parsedLocation[1];
            TestAuthConfig.testAuthAdminFilter.doMockUserOnce("different-rejectUserReadInAnotherPatientCompartment");
            assertThrows( ForbiddenOperationException.class,
                () -> { theClient.read().resource(resourceType).withId(resourceId).execute(); },
                "access not restricted"
            );
        }
    }

    @Test
    public void allowAnyAdminRead() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedReadTests/allowAnyAdminRead/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());

        // Patient Compartment Reads
        for (int indexEntry = 0 ; indexEntry < responseEntries.size() ; indexEntry++) {
            String[] parsedLocation = responseEntries.get(indexEntry).getResponse().getLocation().split("/");
            assertTrue(parsedLocation.length > 1);
            String resourceType = parsedLocation[0];
            String resourceId = parsedLocation[1];
            IBaseResource result = theClient.read().resource(resourceType).withId(resourceId).execute();
            assertNotNull(result);
        }

        // non-Patient Compartment Reads (MessageHeader)
        // not implemented
		/*
		 * IBaseBundle messageHeaderResults =
		 * theClient.search().forResource(MessageHeader.class).where(MessageHeader.FOCUS
		 * .hasId(new IdDt("Patient", "test-allowAnyAdminRead"))).execute();
		 * assertTrue(messageHeaderResults instanceof Bundle);
		 * 
		 * Bundle messageHeaderResultsBundle = (Bundle) messageHeaderResults;
		 * assertEquals(1, messageHeaderResultsBundle.getEntry().size()); String
		 * resourceId =
		 * messageHeaderResultsBundle.getEntryFirstRep().getResource().getIdElement().
		 * getIdPart(); IBaseResource result =
		 * theClient.read().resource(MessageHeader.class).withId(resourceId).execute();
		 * assertNotNull(result);
		 */
    }

}
