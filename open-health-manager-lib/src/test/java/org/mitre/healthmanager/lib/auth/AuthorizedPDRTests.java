package org.mitre.healthmanager.lib.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.TestApplication;
import org.mitre.healthmanager.lib.AuthorizationUtils;
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
public class AuthorizedPDRTests extends TestCaseRoot {

    public AuthorizedPDRTests() {
        IRestfulClientFactory factory = ourCtx.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        factory.setSocketTimeout(1200 * 1000);
    }

    @LocalServerPort
    private int port = 0;


    @Test
    public void allowUserPostPatientDataReceipt() {
        IGenericClient theClient = getClient(port);

        // create patient (as admin)
        AuthorizationUtils.mockAdminUser();
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("allowUserPostPatientDataReceipt");
        patientCreate.addName().setFamily("allowUserPostPatientDataReceipt").addGiven("test");
        patientCreate.setId("test-allowUserPostPatientDataReceipt");

        AuthorizationUtils.mockPatientUser("test-allowUserPostPatientDataReceipt");
        Bundle pdrBundle = getBundle("healthmanager/lib/auth/AuthorizedPDRTests/allowUserPostPatientDataReceipt/Bundle_PDR.json");
        Bundle responseBundle = theClient
            .operation()
            .processMessage()
            .setMessageBundle(pdrBundle)
            .synchronous(Bundle.class)
            .execute();
        assertNotNull(responseBundle);
        
    }

}
