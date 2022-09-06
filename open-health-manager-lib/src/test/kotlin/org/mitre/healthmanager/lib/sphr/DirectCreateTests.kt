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
package org.mitre.healthmanager.lib.sphr

import ca.uhn.fhir.context.FhirContext
import org.mitre.healthmanager.TestApplication
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mitre.healthmanager.lib.dataMgr.pdrAccountExtension
import org.mitre.healthmanager.lib.dataMgr.pdrLinkListExtensionURL
import org.mitre.healthmanager.lib.dataMgr.usernameSystem
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
class DirectCreateTests {

    private val ourLog = LoggerFactory.getLogger(DirectCreateTests::class.java)
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
    fun testPatientCreate() {
        val methodName = "testPatientCreate"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "patientDirectCreation"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.addName().setFamily("patientDirectCreation").addGiven("Test")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(createPatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)
        Assertions.assertTrue(outcome!!.created)
        Assertions.assertTrue(outcome.resource is Patient)

        val patientId = outcome.resource.idElement.idPart
        val patientInstance = (outcome.resource as Patient)

        Assertions.assertTrue(patientInstance.meta.hasExtension(pdrAccountExtension))
        Assertions.assertEquals(testUsername, patientInstance.meta.getExtensionByUrl(pdrAccountExtension).value.toString())
        Assertions.assertTrue(patientInstance.meta.hasExtension(pdrLinkListExtensionURL))
        val pdrLinkList = patientInstance.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertEquals(1, pdrLinkList.extension.size)
        Assertions.assertTrue(pdrLinkList.extension[0].value is Reference)
        val bundleRef = pdrLinkList.extension[0].value as Reference

        // get the Bundle associated with the stored observations
        val bundleId = bundleRef.reference.substringAfter("/")
        val pdrBundle = testClient
            .read()
            .resource(Bundle::class.java)
            .withId(bundleId)
            .execute()
        Assertions.assertEquals(2, pdrBundle.entry.size)
        for ((index, entry) in pdrBundle.entry.withIndex()) {
            when {
                (index == 0) -> {
                    Assertions.assertTrue(entry.resource is MessageHeader)
                }
                (index == 1) -> {
                    Assertions.assertTrue(entry.resource is Patient)
                    // not working due to subsequent read?
                    // Assertions.assertEquals(patientInstance.idElement.toString(), entry.linkFirstRep.url)
                    Assertions.assertEquals(patientInstance.idElement.toString().substringAfter(patientInstance.idElement.baseUrl + "/"), entry.linkFirstRep.url)

                }
            }
        }

        // get the MessageHeader
        val messageHeader = testClient
            .read()
            .resource(MessageHeader::class.java)
            .withId(pdrBundle.entry[0].linkFirstRep.url.split("/")[1])
            .execute()
        Assertions.assertEquals(2, messageHeader.focus.size)
        var sawPatient = false
        var sawBundle = false
        messageHeader.focus.forEach {
            val typeAndId = it.reference.split("/")
            if (typeAndId[0] == "Patient") {
                sawPatient = true
                Assertions.assertEquals(patientId, typeAndId[1])
            }
            if (typeAndId[0] == "Bundle") {
                sawBundle = true
                Assertions.assertEquals(bundleId, typeAndId[1])
            }
        }
        Assertions.assertTrue(sawPatient)
        Assertions.assertTrue(sawBundle)


    }

    @Test
    fun testPatientCreateWithId() {
        val methodName = "testPatientCreateWithId"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "patientDirectCreationWithId"
        val testPatientId = "test-patientDirectCreationWithId"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.addName().setFamily("patientDirectCreation").addGiven("Test")
        createPatient.id = testPatientId

        val outcome: MethodOutcome? = try {
            testClient.update()
                .resource(createPatient)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)


        val patientId = outcome!!.resource.idElement.idPart
        Assertions.assertEquals(testPatientId, patientId)

    }

    @Test
    fun testUpdateUsernameWithPostFail() {
        val methodName = "testUpdateUsernameWithPostFail"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "updateUsernameWithPost"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.addName().setFamily("patientDirectCreation").addGiven("Test")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(createPatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)

        val outcome2: MethodOutcome? = try {
            testClient.create()
                .resource(createPatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            null
        }
        Assertions.assertNull(outcome2)

    }

    @Test
    fun testUpdateUsernameWithPutDifferentIdFail() {
        val methodName = "testUpdateUsernameWithPutDifferentIdFail"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "updateUsernameWithPutDifferentId"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.addName().setFamily("patientDirectCreation").addGiven("Test")

        val outcome: MethodOutcome? = try {
            testClient.create()
                .resource(createPatient)
                .prettyPrint()
                .encodedJson()
                .execute()
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)

        val updatePatient = Patient()
        updatePatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        updatePatient.addName().setFamily("patientDirectCreation").addGiven("Test")
        updatePatient.id = "different"
        val outcome2: MethodOutcome? = try {
            testClient.update()
                .resource(updatePatient)
                .execute();
        } catch (e: Exception) {
            null
        }
        Assertions.assertNull(outcome2)

    }

    @Test
    fun testPatientUpdateExistingId() {
        val methodName = "testPatientUpdateExistingId"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testPatientUpdateExistingId"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.addName().setFamily("patientDirectCreation").addGiven("Test")
        createPatient.id = "test-patientId"

        val outcome: MethodOutcome? = try {
            testClient.update()
                .resource(createPatient)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)

        val patientId = outcome!!.resource.idElement.idPart
        Assertions.assertEquals("test-patientId", patientId)

        val updatePatient = Patient()
        updatePatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        updatePatient.addName().setFamily("patientDirectCreation").addGiven("Updated")
        updatePatient.id = "test-patientId"

        val outcomeUpdate: MethodOutcome? = try {
            testClient.update()
                .resource(updatePatient)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("update failed")
            null
        }
        Assertions.assertNotNull(outcomeUpdate)


        val patientInstance = testClient
            .read()
            .resource(Patient::class.java)
            .withId(patientId)
            .execute()

        // validate update
        Assertions.assertEquals("Updated", patientInstance.nameFirstRep.givenAsSingleString)

    }

    @Test
    fun testTwoWrites() {
        val methodName = "testTwoWrites"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "directWriteTwo"

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", testUsername))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // Submit the bundle
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/PDR_CreatePatient.json")
        )
        val response : Bundle = testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundle)
            .synchronous(Bundle::class.java)
            .execute()

        Assertions.assertEquals(1, response.entry.size)
        when (val firstResource = response.entry[0].resource) {
            is MessageHeader -> {
                Assertions.assertEquals(firstResource.response.code, MessageHeader.ResponseType.OK)
            }
            else -> {
                Assertions.fail("response doesn't have a message header")
            }
        }

        // make sure patient does exist
        val patientId: String? = searchForPatientByUsername(testUsername, testClient, 120)
        Assertions.assertNotNull(patientId)

        val observation: Observation = ourCtx.newJsonParser().parseResource(
            Observation::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/Observation_Write.json")
        )
        observation.subject = Reference("Patient/$patientId")

        /// create twice in quick succession
        val responseOne = testClient
            .create()
            .resource(observation)
            .prettyPrint()
            .encodedJson()
            .execute()
            .resource
        val responseTwo = testClient
            .create()
            .resource(observation)
            .prettyPrint()
            .encodedJson()
            .execute()
            .resource
        val observationOne = if (responseOne is Observation) responseOne else null
        val observationTwo = if (responseTwo is Observation) responseTwo else null

        // checks
        // - each observation has a different id, username extension in meta, and list of bundles
        // - both bundles are the same
        Assertions.assertNotNull(observationOne)
        Assertions.assertNotNull(observationTwo)
        Assertions.assertTrue(observationOne!!.meta.hasExtension(pdrAccountExtension))
        Assertions.assertEquals(testUsername, observationOne.meta.getExtensionByUrl(pdrAccountExtension).value.toString())
        Assertions.assertTrue(observationTwo!!.meta.hasExtension(pdrAccountExtension))
        Assertions.assertEquals(testUsername, observationTwo.meta.getExtensionByUrl(pdrAccountExtension).value.toString())
        Assertions.assertTrue(observationOne.meta.hasExtension(pdrLinkListExtensionURL))
        Assertions.assertTrue(observationTwo.meta.hasExtension(pdrLinkListExtensionURL))
        val pdrLinkListOne = observationOne.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertEquals(1, pdrLinkListOne.extension.size)
        Assertions.assertTrue(pdrLinkListOne.extension[0].value is Reference)
        val pdrLinkListTwo = observationTwo.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertEquals(1, pdrLinkListTwo.extension.size)
        Assertions.assertTrue(pdrLinkListTwo.extension[0].value is Reference)
        val bundleRefOne = pdrLinkListOne.extension[0].value as Reference
        val bundleRefTwo = pdrLinkListTwo.extension[0].value as Reference
        Assertions.assertEquals(bundleRefOne.reference, bundleRefTwo.reference)

        // get the Bundle associated with the stored observations
        val bundleId = bundleRefOne.reference.substringAfter("/")
        val pdrBundle = testClient
            .read()
            .resource(Bundle::class.java)
            .withId(bundleId)
            .execute()
        Assertions.assertEquals(3, pdrBundle.entry.size)
        for ((index, entry) in pdrBundle.entry.withIndex()) {
            when {
                (index == 0) -> {
                    Assertions.assertTrue(entry.resource is MessageHeader)
                }
                (index == 1) -> {
                    Assertions.assertTrue(entry.resource is Observation)
                    Assertions.assertEquals(responseOne.idElement.toString(), entry.linkFirstRep.url)
                }
                (index == 2) -> {
                    Assertions.assertTrue(entry.resource is Observation)
                    Assertions.assertEquals(responseTwo.idElement.toString(), entry.linkFirstRep.url)
                }
            }
        }

        // get the MessageHeader
        val messageHeader = testClient
            .read()
            .resource(MessageHeader::class.java)
            .withId(pdrBundle.entry[0].linkFirstRep.url.split("/")[1])
            .execute()
        Assertions.assertEquals(2, messageHeader.focus.size)
        var sawPatient = false
        var sawBundle = false
        messageHeader.focus.forEach {
            val typeAndId = it.reference.split("/")
            if (typeAndId[0] == "Patient") {
                sawPatient = true
                Assertions.assertEquals(patientId, typeAndId[1])
            }
            if (typeAndId[0] == "Bundle") {
                sawBundle = true
                Assertions.assertEquals(bundleId, typeAndId[1])
            }
        }
        Assertions.assertTrue(sawPatient)
        Assertions.assertTrue(sawBundle)
    }

    @Test
    fun testInferUsernameFromPatientLinkage() {
        val methodName = "testInferUsernameFromPatientLinkage"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "inferUsernameViaLink"

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", testUsername))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // Submit the bundle
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/InferUsernameFromLink_PDR_CreatePatient.json")
        )
        val response : Bundle = testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundle)
            .synchronous(Bundle::class.java)
            .execute()

        Assertions.assertEquals(1, response.entry.size)
        when (val firstResource = response.entry[0].resource) {
            is MessageHeader -> {
                Assertions.assertEquals(firstResource.response.code, MessageHeader.ResponseType.OK)
            }
            else -> {
                Assertions.fail("response doesn't have a message header")
            }
        }

        // make sure patient does exist
        val patientId: String? = searchForPatientByUsername(testUsername, testClient, 120)
        Assertions.assertNotNull(patientId)

        val observation: Observation = ourCtx.newJsonParser().parseResource(
            Observation::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/InferUsernameFromLink_Observation_Write.json")
        )
        observation.subject = Reference("Patient/$patientId")

        /// create twice in quick succession
        val responseOne = testClient
            .create()
            .resource(observation)
            .prettyPrint()
            .encodedJson()
            .execute()
            .resource
        val observationOne = if (responseOne is Observation) responseOne else null

        // check that username inferred correctly
        Assertions.assertNotNull(observationOne)
        Assertions.assertTrue(observationOne!!.meta.hasExtension(pdrAccountExtension))
        Assertions.assertEquals(testUsername, observationOne.meta.getExtensionByUrl(pdrAccountExtension).value.toString())

    }

    @Test
    fun testSharedResourceWrite() {
        val methodName = "testSharedResourceWrite"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "sharedResourceWrite"

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", testUsername))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // Submit the bundle
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/SharedResource_PDR_CreatePatient.json")
        )
        val response : Bundle = testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundle)
            .synchronous(Bundle::class.java)
            .execute()

        Assertions.assertEquals(1, response.entry.size)
        when (val firstResource = response.entry[0].resource) {
            is MessageHeader -> {
                Assertions.assertEquals(firstResource.response.code, MessageHeader.ResponseType.OK)
            }
            else -> {
                Assertions.fail("response doesn't have a message header")
            }
        }

        // make sure patient does exist
        val patientId: String? = searchForPatientByUsername(testUsername, testClient, 120)
        Assertions.assertNotNull(patientId)

        val practitioner: Practitioner = ourCtx.newJsonParser().parseResource(
            Practitioner::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/SharedResource_Practitioner_Write.json")
        )
        val observation: Observation = ourCtx.newJsonParser().parseResource(
            Observation::class.java, stringFromResource("healthmanager/sphr/DirectCreateTests/SharedResource_Observation_Write.json")
        )
        observation.subject = Reference("Patient/$patientId")

        /// create twice in quick succession
        val responseOne = testClient
            .create()
            .resource(practitioner)
            .prettyPrint()
            .encodedJson()
            .execute()
            .resource
        val practitionerOne = if (responseOne is Practitioner) responseOne else null
        Assertions.assertNotNull(practitionerOne)
        observation.performer.add(Reference("Practitioner/" + practitionerOne!!.idElement.idPart))
        val responseTwo = testClient
            .create()
            .resource(observation)
            .prettyPrint()
            .encodedJson()
            .execute()
            .resource

        // checks
        // - each observation has a different id, username extension in meta, and list of bundles
        // - both bundles are the same
        val observationTwo = if (responseTwo is Observation) responseTwo else null
        Assertions.assertNotNull(observationTwo)
        Assertions.assertFalse(practitionerOne.meta.hasExtension(pdrAccountExtension))
        Assertions.assertTrue(observationTwo!!.meta.hasExtension(pdrAccountExtension))
        Assertions.assertEquals(testUsername, observationTwo.meta.getExtensionByUrl(pdrAccountExtension).value.toString())
        Assertions.assertFalse(practitionerOne.meta.hasExtension(pdrLinkListExtensionURL))
        Assertions.assertTrue(observationTwo.meta.hasExtension(pdrLinkListExtensionURL))
        val pdrLinkListTwo = observationTwo.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertEquals(1, pdrLinkListTwo.extension.size)
        Assertions.assertTrue(pdrLinkListTwo.extension[0].value is Reference)
        val bundleRefTwo = pdrLinkListTwo.extension[0].value as Reference

        // get the Bundle associated with the stored observations
        val bundleId = bundleRefTwo.reference.substringAfter("/")
        val pdrBundle = testClient
            .read()
            .resource(Bundle::class.java)
            .withId(bundleId)
            .execute()
        Assertions.assertEquals(2, pdrBundle.entry.size)
        for ((index, entry) in pdrBundle.entry.withIndex()) {
            when {
                (index == 0) -> {
                    Assertions.assertTrue(entry.resource is MessageHeader)
                }
                (index == 1) -> {
                    Assertions.assertTrue(entry.resource is Observation)
                    Assertions.assertEquals(responseTwo.idElement.toString(), entry.linkFirstRep.url)
                }
            }
        }

        // get the MessageHeader
        val messageHeader = testClient
            .read()
            .resource(MessageHeader::class.java)
            .withId(pdrBundle.entry[0].linkFirstRep.url.split("/")[1])
            .execute()
        Assertions.assertEquals(2, messageHeader.focus.size)
        var sawPatient = false
        var sawBundle = false
        messageHeader.focus.forEach {
            val typeAndId = it.reference.split("/")
            if (typeAndId[0] == "Patient") {
                sawPatient = true
                Assertions.assertEquals(patientId, typeAndId[1])
            }
            if (typeAndId[0] == "Bundle") {
                sawBundle = true
                Assertions.assertEquals(bundleId, typeAndId[1])
            }
        }
        Assertions.assertTrue(sawPatient)
        Assertions.assertTrue(sawBundle)
    }

}

