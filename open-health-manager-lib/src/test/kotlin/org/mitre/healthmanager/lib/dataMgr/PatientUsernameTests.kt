/*
Copyright 2022 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package org.mitre.healthmanager.lib.dataMgr

import ca.uhn.fhir.context.FhirContext
import org.mitre.healthmanager.TestApplication
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mitre.healthmanager.searchForPatientByUsername
import org.mitre.healthmanager.stringFromResource
import org.mitre.healthmanager.getAdminAuthClient
import org.mitre.healthmanager.TestUtils.mockAdminUser
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort


@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestApplication::class],
    properties = [
        "spring.batch.job.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:dbr4",
        "spring.datasource.username=sa",
        "spring.datasource.password=null",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=ca.uhn.fhir.jpa.model.dialect.HapiFhirH2Dialect",
        "hapi.fhir.enable_repository_validating_interceptor=true",
        "hapi.fhir.fhir_version=r4",
    ]
)
class PatientUsernameTests {

    private val ourLog = LoggerFactory.getLogger(PatientUsernameTests::class.java)
    private val ourCtx: FhirContext = FhirContext.forR4()
    init {
        ourCtx.restfulClientFactory.serverValidationMode = ServerValidationModeEnum.NEVER
        ourCtx.restfulClientFactory.socketTimeout = 1200 * 1000
    }

    @LocalServerPort
    private var port = 0

    @BeforeEach
    fun setAdminAuthContext() {
        mockAdminUser()
    }

    @Test
    fun testCreateWithoutUsername() {
        val methodName = "testCreateWithoutUsername"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        val noUsernamePatient = Patient()
        noUsernamePatient.addName().setFamily("Smith").addGiven("John")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(noUsernamePatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            Assertions.assertTrue(e is UnprocessableEntityException)
            null
        }
        Assertions.assertNull(outcome)
    }

    @Test
    fun testCreateWithoutUsernameOnPatientRecordPDR() {
        val methodName = "testCreateWithoutUsernameOnPatientRecordPDR"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", "noUsernameOnPt"))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // file test data
        // has username identifier and first / last name
        val pdrBundle: Bundle = ourCtx.newJsonParser().parseResource<Bundle>(
            Bundle::class.java, stringFromResource("healthmanager/dataMgr/PatientUsernameTests/PatientRecordNoUsernamePDR.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(pdrBundle)
            .synchronous(Bundle::class.java)
            .execute()

        // make sure patient does exist now
        val patientCreatedId: String? = searchForPatientByUsername("noUsernameOnPt", testClient, 120)
        Assertions.assertNotNull(patientCreatedId)

        // read
        val patient: Patient = testClient.read()
            .resource(Patient::class.java)
            .withId(patientCreatedId)
            .execute()

        Assertions.assertEquals("urn:mitre:healthmanager:account:username", patient.identifierFirstRep.system)
        Assertions.assertEquals("noUsernameOnPt", patient.identifierFirstRep.value)
    }

    @Test
    fun testUpdateWithoutUsername() {
        val methodName = "testUpdateWithoutUsername"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testCreate"

        // create
        val usernamePatient = Patient()
        usernamePatient.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue(testUsername)
        usernamePatient.addName().setFamily("Smith").addGiven("John")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(usernamePatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            null
        }
        Assertions.assertNotNull(outcome)
        val newId = outcome!!.resource.idElement.idPart

        // update
        val noUsernamePatient = Patient()
        noUsernamePatient.id = "Patient/$newId"
        noUsernamePatient.addName().setFamily("Smith").addGiven("John").addGiven("M")

        val outcomePut: MethodOutcome? = try {
            testClient.update()
                .resource(noUsernamePatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            null
        }
        Assertions.assertNotNull(outcomePut)

        // read
        val patient: Patient = testClient.read()
            .resource(Patient::class.java)
            .withId(newId)
            .execute()

        Assertions.assertEquals(2, patient.nameFirstRep.given.size)
        Assertions.assertEquals(1, patient.identifier.size)
        Assertions.assertEquals(usernameSystem, patient.identifierFirstRep.system)
        Assertions.assertEquals(testUsername, patient.identifierFirstRep.value)
    }

    @Test
    fun testUpdateToDifferentUsername() {
        val methodName = "testUpdateToDifferentUsername"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        // create
        val usernamePatient = Patient()
        usernamePatient.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("testBadUpdate")
        usernamePatient.addName().setFamily("Smith").addGiven("John")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(usernamePatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            null
        }
        Assertions.assertNotNull(outcome)
        val newId = outcome!!.resource.idElement.idPart

        // update
        val noUsernamePatient = Patient()
        noUsernamePatient.id = "Patient/$newId"
        noUsernamePatient.addIdentifier().setSystem("urn:mitre:healthmanager:account:username").setValue("testBadUpdateDifferent")
        noUsernamePatient.addName().setFamily("Smith").addGiven("John").addGiven("M")

        val outcomePut: MethodOutcome? = try {
            testClient.update()
                .resource(noUsernamePatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            Assertions.assertTrue(e is UnprocessableEntityException)
            null
        }
        Assertions.assertNull(outcomePut)

        // read
        val patient: Patient = testClient.read()
            .resource(Patient::class.java)
            .withId(newId)
            .execute()

        Assertions.assertEquals(1, patient.nameFirstRep.given.size)
    }
}