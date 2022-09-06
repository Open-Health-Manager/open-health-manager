package org.mitre.healthmanager.lib.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.mitre.healthmanager.TestApplication;
import org.mitre.healthmanager.lib.AuthorizationUtils;
import org.mitre.healthmanager.lib.TestCaseRoot;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;

import ca.uhn.fhir.rest.api.MethodOutcome;
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
public class AuthorizedWriteTests extends TestCaseRoot {

    public AuthorizedWriteTests() {
        IRestfulClientFactory factory = ourCtx.getRestfulClientFactory();
        factory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        factory.setSocketTimeout(1200 * 1000);
    }

    @LocalServerPort
    private int port = 0;

    @Test
    public void allowUserDirectTheirPatientCreateViaPUT() {
        IGenericClient theClient = getClient(port);
        
        // create the user's patient as user - should suceed
        AuthorizationUtils.mockPatientUser("test-allowUserDirectTheirPatientCreateViaPUT");
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("allowUserDirectTheirPatientCreateViaPUT");
        patientCreate.addName().setFamily("allowUserDirectTheirPatientCreateViaPUT").addGiven("test");
        patientCreate.setId("test-allowUserDirectTheirPatientCreateViaPUT");
        MethodOutcome createOutcome = theClient.update()
                .resource(patientCreate)
                .prettyPrint()
                .encodedJson()
                .execute();
        assertNotNull(createOutcome);
        assertTrue(createOutcome.getResource() instanceof Patient);

    }

    @Test
    public void rejectUserDirectDifferentPatientCreateViaPUT() {
        IGenericClient theClient = getClient(port);
        
        // create the user's patient as user - should suceed
        AuthorizationUtils.mockPatientUser("test-rejectUserDirectDifferentPatientCreateViaPUT");
    
        // create a different patient as user - should fail
        Patient differentPatientCreate = new Patient();
        differentPatientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("rejectUserDirectDifferentPatientCreateViaPUT");
        differentPatientCreate.addName().setFamily("rejectUserDirectDifferentPatientCreateViaPUT").addGiven("test");
        differentPatientCreate.setId("different-rejectUserDirectDifferentPatientCreateViaPUT");
        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.update()
                .resource(differentPatientCreate)
                .prettyPrint()
                .encodedJson()
                .execute(); },
            "access not restricted"
        );
    }


    @Test
    public void allowUserDirectWriteInPatientCompartment() {
        IGenericClient theClient = getClient(port);
        
        // create patient (as admin)
        AuthorizationUtils.mockAdminUser();
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("allowUserDirectWriteInPatientCompartment");
        patientCreate.addName().setFamily("allowUserDirectWriteInPatientCompartment").addGiven("test");
        patientCreate.setId("test-allowUserDirectWriteInPatientCompartment");
        
        MethodOutcome patientCreateOutcome = theClient.update()
            .resource(patientCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(patientCreateOutcome);
        assertTrue(patientCreateOutcome.getResource() instanceof Patient);
        
        // create non-patient resource in the patient compartment
        // use Encounter as an example
        AuthorizationUtils.mockPatientUser("test-allowUserDirectWriteInPatientCompartment");
        Encounter encounterCreate = new Encounter();
        encounterCreate.setSubject(new Reference("Patient/test-allowUserDirectWriteInPatientCompartment"));

        MethodOutcome encounterCreateOutcome = theClient.create()
            .resource(encounterCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(encounterCreateOutcome);
        assertTrue(encounterCreateOutcome.getResource() instanceof Encounter);

        // update non-patient resource in the patient compartment
        // use Encounter as an example
        Encounter encounterUpdate = (Encounter) encounterCreateOutcome.getResource();
        encounterUpdate.setStatus(Encounter.EncounterStatus.FINISHED);

        MethodOutcome encounterUpdateOutcome = theClient.update()
            .resource(encounterUpdate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(encounterUpdateOutcome);
        assertTrue(encounterUpdateOutcome.getResource() instanceof Encounter);
        assertEquals(Encounter.EncounterStatus.FINISHED, ((Encounter) encounterUpdateOutcome.getResource()).getStatus());
        

    }

    @Test
    public void rejectUserDirectWriteOutsidePatientCompartment() {
        IGenericClient theClient = getClient(port);
        
        // create patient (as admin)
        AuthorizationUtils.mockAdminUser();
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("rejectUserDirectWriteOutsidePatientCompartment");
        patientCreate.addName().setFamily("rejectUserDirectWriteOutsidePatientCompartment").addGiven("test");
        patientCreate.setId("test-rejectUserDirectWriteOutsidePatientCompartment");
        
        MethodOutcome patientCreateOutcome = theClient.update()
            .resource(patientCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(patientCreateOutcome);
        assertTrue(patientCreateOutcome.getResource() instanceof Patient);

        // create non-patient resource outside the patient compartment
        // use Practitioner as an example
        AuthorizationUtils.mockPatientUser("test-rejectUserDirectWriteOutsidePatientCompartment");
        Practitioner practitionerCreate = new Practitioner();
        practitionerCreate.addName().setFamily("rejectUserDirectWriteOutsidePatientCompartment").addGiven("test");

        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.create()
                .resource(practitionerCreate)
                .prettyPrint()
                .encodedJson()
                .execute(); },
            "access not restricted"
        );

        // update non-patient resource outside the patient compartment
        // use Practitioner as an example

        // create as admin
        AuthorizationUtils.mockAdminUser();
        MethodOutcome practitionerCreateOutcome = theClient.create()
            .resource(practitionerCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(practitionerCreateOutcome);
        assertTrue(practitionerCreateOutcome.getResource() instanceof Practitioner);

        AuthorizationUtils.mockPatientUser("test-rejectUserDirectWriteOutsidePatientCompartment");
        Practitioner practitionerUpdate = (Practitioner) practitionerCreateOutcome.getResource();
        practitionerUpdate.setActive(true);

        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.update()
                .resource(practitionerUpdate)
                .prettyPrint()
                .encodedJson()
                .execute(); },
            "access not restricted"
        );

    }

    @Test
    public void rejectUserDirectWriteInAnotherPatientCompartment() {
        IGenericClient theClient = getClient(port);
        
        // create patient (as admin)
        AuthorizationUtils.mockAdminUser();
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("rejectUserDirectWriteInAnotherPatientCompartment");
        patientCreate.addName().setFamily("rejectUserDirectWriteInAnotherPatientCompartment").addGiven("test");
        patientCreate.setId("test-rejectUserDirectWriteInAnotherPatientCompartment");
        
        MethodOutcome patientCreateOutcome = theClient.update()
            .resource(patientCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(patientCreateOutcome);
        assertTrue(patientCreateOutcome.getResource() instanceof Patient);
        
        // create non-patient resource in a different patient's compartment
        // use Encounter as an example
        AuthorizationUtils.mockPatientUser("different-rejectUserDirectWriteInAnotherPatientCompartment");
        Encounter encounterCreate = new Encounter();
        encounterCreate.setSubject(new Reference("Patient/test-rejectUserDirectWriteInAnotherPatientCompartment"));

        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.create()
                .resource(encounterCreate)
                .prettyPrint()
                .encodedJson()
                .execute(); },
            "access not restricted"
        );

        // update non-patient resource in a different patient's compartment
        // use Encounter as an example
        
        // first create as admin
        AuthorizationUtils.mockAdminUser();
        MethodOutcome encounterCreateOutcome = theClient.create()
            .resource(encounterCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(encounterCreateOutcome);
        assertTrue(encounterCreateOutcome.getResource() instanceof Encounter);

        // update as different user
        AuthorizationUtils.mockPatientUser("different-rejectUserDirectWriteInAnotherPatientCompartment");
        Encounter encounterUpdate = (Encounter) encounterCreateOutcome.getResource();
        encounterUpdate.setStatus(Encounter.EncounterStatus.FINISHED);

        assertThrows( ForbiddenOperationException.class,
            () -> { theClient.update()
                .resource(encounterUpdate)
                .prettyPrint()
                .encodedJson()
                .execute(); },
            "access not restricted"
        );
    }

    @Test
    public void allowAdminAnyWrite() {
        IGenericClient theClient = getClient(port);
        
        // create patient (as admin)
        AuthorizationUtils.mockAdminUser();
        Patient patientCreate = new Patient();
        patientCreate.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("allowAdminAnyWrite");
        patientCreate.addName().setFamily("allowAdminAnyWrite").addGiven("test");
        patientCreate.setId("test-allowAdminAnyWrite");

        MethodOutcome patientCreateOutcome = theClient.update()
            .resource(patientCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(patientCreateOutcome);
        assertTrue(patientCreateOutcome.getResource() instanceof Patient);
        
        Practitioner practitionerCreate = new Practitioner();
        practitionerCreate.addName().setFamily("allowAdminAnyWrite").addGiven("test");

        MethodOutcome practitionerCreateOutcome = theClient.create()
            .resource(practitionerCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(practitionerCreateOutcome);
        assertTrue(practitionerCreateOutcome.getResource() instanceof Practitioner);

        Encounter encounterCreate = new Encounter();
        encounterCreate.setSubject(new Reference("Patient/test-allowAdminAnyWrite"));
        MethodOutcome encounterCreateOutcome = theClient.create()
            .resource(encounterCreate)
            .prettyPrint()
            .encodedJson()
            .execute();
        assertNotNull(encounterCreateOutcome);
        assertTrue(encounterCreateOutcome.getResource() instanceof Encounter);

    }

}
