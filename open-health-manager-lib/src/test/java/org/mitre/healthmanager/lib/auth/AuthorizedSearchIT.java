package org.mitre.healthmanager.lib.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
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
public class AuthorizedSearchIT extends TestCaseRoot {

    public AuthorizedSearchIT() {
        IRestfulClientFactory factory = ourCtx.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        factory.setSocketTimeout(1200 * 1000);
    }

    @LocalServerPort
    private int port = 0;

    @Test
    // Searches for encounter as an example of a patient compartment resource type
    public void allowUserSearchInPatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/allowUserSearchInPatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());

        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-allowUserSearchInPatientCompartment");
        IBaseBundle encounterResults = theClient.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(new IdDt("Patient", "test-allowUserSearchInPatientCompartment"))).execute();
        assertTrue(encounterResults instanceof Bundle);
        Bundle encounterResultsBundle = (Bundle) encounterResults;
        assertEquals(1, encounterResultsBundle.getEntry().size());
    }

    @Test
    // Searches for PDR Message Headers as an example of a non-Patient Compartment search
    // NOTE: this may be allowed in the future
    public void rejectUserSearchOutsidePatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/rejectUserSearchOutsidePatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());

        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-rejectUserSearchOutsidePatientCompartment");
        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.search().forResource(MessageHeader.class).where(MessageHeader.FOCUS.hasId(new IdDt("Patient", "test-rejectUserSearchOutsidePatientCompartment"))).execute(); },
            "access not restricted"
        );
    }

    @Test
    // Searches for encounter as an example of a patient compartment resource type
    public void rejectUserSearchInAnotherPatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/rejectUserSearchInAnotherPatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());

        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("different-rejectUserSearchOutsidePatientCompartment");
        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(new IdDt("Patient", "test-rejectUserSearchOutsidePatientCompartment"))).execute(); },
            "access not restricted"
        );
    }

    @Test
    // Searches for encounter as an example of a patient compartment resource type
    public void narrowUserSearchToTheirPatientCompartment() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/narrowUserSearchToTheirPatientCompartment/Bundle_create.json");
        Bundle createResult = submitTransactionAsAdmin(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());

        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-narrowUserSearchToTheirPatientCompartment");
        IBaseBundle encounterResults = theClient.search().forResource(Encounter.class).execute();
        assertTrue(encounterResults instanceof Bundle);
        Bundle encounterResultsBundle = (Bundle) encounterResults;
        // make sure that the search has been appropriately narrowed
        assertTrue(encounterResultsBundle.hasLink());
        assertTrue(encounterResultsBundle.getLinkFirstRep().getUrl().contains("test-narrowUserSearchToTheirPatientCompartment"));
        assertEquals(1, encounterResultsBundle.getEntry().size());
        Resource resultResource = encounterResultsBundle.getEntryFirstRep().getResource();
        assertTrue(resultResource instanceof Encounter);
        Reference subjectReference = ((Encounter) resultResource).getSubject();
        assertEquals("Patient/test-narrowUserSearchToTheirPatientCompartment", subjectReference.getReference());
    }

    @Test
    // Tests patient-specific searches and general searches
    // NOTE: if we change to a patient-specific partitioning strategy, the general searches may fail
    public void allowAnyAdminSearch() {
        IGenericClient theClient = getClient(port);
        Bundle createBundle1 = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/allowAnyAdminSearch/Bundle_create1.json");
        Bundle createResult1 = submitTransactionAsAdmin(createBundle1, theClient);
        List<BundleEntryComponent> responseEntries1 = createResult1.getEntry();
        assertEquals(createBundle1.getEntry().size(), responseEntries1.size());
        Bundle createBundle2 = getBundle("healthmanager/lib/auth/AuthorizedSearchTests/allowAnyAdminSearch/Bundle_create2.json");
        Bundle createResult2 = submitTransactionAsAdmin(createBundle2, theClient);
        List<BundleEntryComponent> responseEntries2 = createResult2.getEntry();
        assertEquals(createBundle2.getEntry().size(), responseEntries2.size());

        // search with patient context in a specific patient compartment
        IBaseBundle encounterResults = theClient.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(new IdDt("Patient", "test-allowAnyAdminSearch1"))).execute();
        assertTrue(encounterResults instanceof Bundle);
        Bundle encounterResultsBundle = (Bundle) encounterResults;
        assertEquals(1, encounterResultsBundle.getEntry().size());

        // search with patient context outside a patient compartment
        // not implemented
		/*
		 * IBaseBundle messageHeaderResults =
		 * theClient.search().forResource(MessageHeader.class).where(MessageHeader.FOCUS
		 * .hasId(new IdDt("Patient", "test-allowAnyAdminSearch1"))).execute();
		 * assertTrue(messageHeaderResults instanceof Bundle); Bundle
		 * messageHeaderResultsBundle = (Bundle) messageHeaderResults; assertEquals(1,
		 * messageHeaderResultsBundle.getEntry().size());
		 */

        // search without patient context in the patient compartment
        IBaseBundle allEncounterResults = theClient.search().forResource(Encounter.class).execute();
        assertTrue(allEncounterResults instanceof Bundle);
        Bundle allEncounterResultsBundle = (Bundle) allEncounterResults;
        // multiple entries returned - at least 2 created above, maybe more from other unit tests
        assertTrue(allEncounterResultsBundle.getEntry().size() > 1); 

        // search without patient context outside the patient compartment
        // not implemented - transaction bundles currently not stored in resources
		/*
		 * IBaseBundle bundleResults =
		 * theClient.search().forResource(Bundle.class).execute();
		 * assertTrue(bundleResults instanceof Bundle); Bundle bundleResultsBundle =
		 * (Bundle) bundleResults; // multiple entries returned - at least 2 created
		 * above, maybe more from other unit tests
		 * assertTrue(bundleResultsBundle.getEntry().size() > 1);
		 */

    }

}
