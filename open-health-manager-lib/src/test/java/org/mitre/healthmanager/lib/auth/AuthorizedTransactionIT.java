package org.mitre.healthmanager.lib.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.TestApplication;
import org.mitre.healthmanager.lib.TestCaseRoot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;

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
public class AuthorizedTransactionIT extends TestCaseRoot {

    public AuthorizedTransactionIT() {
        IRestfulClientFactory factory = ourCtx.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        factory.setSocketTimeout(1200 * 1000);
    }

    @LocalServerPort
    private int port = 0;

    @Test
    public void allowUserPostTransaction() {
        IGenericClient theClient = getClient(port);
        
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("allowUserPostTransaction");
        patientCreate.addName().setFamily("allowUserPostTransaction").addGiven("test");
        patientCreate.setId("test-allowUserPostTransaction");
        
        TestAuthConfig.testAuthAdminFilter.doMockUserOnce("test-allowUserPostTransaction");
        Bundle createBundle = getBundle("healthmanager/lib/auth/AuthorizedTransactionTests/allowUserPostTransaction/Bundle_create.json");
        Bundle createResult = submitTransaction(createBundle, theClient);
        List<BundleEntryComponent> responseEntries = createResult.getEntry();
        assertEquals(createBundle.getEntry().size(), responseEntries.size());
    }

}
