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
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mitre.healthmanager.TestUtils.mockAdminUser
import org.mitre.healthmanager.searchForPatientByUsername
import org.mitre.healthmanager.stringFromResource
import org.mitre.healthmanager.getAdminAuthClient
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
class RebuildAccountTests {

    private val ourLog = LoggerFactory.getLogger(RebuildAccountTests::class.java)
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
    fun testPatientOnlyRebuild() {
        val methodName = "testPatientOnlyRebuild"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        // file test data
        // has username identifier and first / last name
        // NOTE: now that transactions get filed as PDRs, this test makes less sense
        // But so does the rebuild operation in general
        val transactionBundle: Bundle = ourCtx.newJsonParser().parseResource<Bundle>(
            Bundle::class.java, stringFromResource("healthmanager/dataMgr/RebuildAccountTests/PatientOnlyBundleTransaction.json")
        )
        testClient.transaction().withBundle(transactionBundle).execute()

        ourLog.info("**** get patient id for username ****")
        // give indexing a few more seconds
        val patId = searchForPatientByUsername("naccount", testClient, 120)

        Assertions.assertNotNull(patId)
        val patResource = testClient.read().resource(Patient::class.java).withId(patId).encodedJson().execute()
        Assertions.assertEquals("Account", patResource.nameFirstRep.family)
        Assertions.assertEquals("New", patResource.nameFirstRep.givenAsSingleString)

        // trigger rebuild (operation)
        // Create the input parameters to pass to the server
        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType("naccount")

        testClient
            .operation()
            .onServer()
            .named("\$rebuild-account")
            .withParameters(inParams)
            .execute()

        // verify updates - first and last names still there
        val patResourceRebuilt = testClient.read().resource(Patient::class.java).withId(patId).encodedJson().execute()
        Assertions.assertEquals(1, patResourceRebuilt.name.size)
        Assertions.assertEquals("Account", patResourceRebuilt.nameFirstRep.family)
        Assertions.assertEquals("New", patResourceRebuilt.nameFirstRep.givenAsSingleString)
    }

    @Test
    fun testOnePDRRebuild() {
        val methodName = "testOnePDRRebuild"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        // file test data
        // has username identifier and first / last name
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource<Bundle>(
            Bundle::class.java, stringFromResource("healthmanager/dataMgr/RebuildAccountTests/SinglePDRRebuild.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundle)
            .synchronous(Bundle::class.java)
            .execute()

        ourLog.info("**** get patient id for username ****")
        // give indexing a few more seconds
        val patientId = searchForPatientByUsername("rebuildonepdr", testClient, 120)

        Assertions.assertNotNull(patientId)
        val patResource = testClient.read().resource(Patient::class.java).withId(patientId).encodedJson().execute()
        Assertions.assertEquals("Rebuild", patResource.nameFirstRep.family)
        Assertions.assertEquals("OnePDR", patResource.nameFirstRep.givenAsSingleString)

        // check other resources
        var originalIdPatient : String? = null
        var originalIdMessageHeader : String? = null
        var originalIdBundle : String? = null
        var originalIdEncounter : String? = null
        var originalIdProcedure : String? = null
        val patientEverythingResultOriginal : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", patientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResultOriginal.parameter.size)
        when (val everythingBundle = patientEverythingResultOriginal.parameter[0].resource) {
            is Bundle -> {
                // 3 entries stored from the bundle
                Assertions.assertEquals(5, everythingBundle.entry.size)
                everythingBundle.entry.forEach { entry ->
                    when (val resource = entry.resource) {
                        is Patient -> { originalIdPatient = resource.idElement.idPart}
                        is MessageHeader -> { originalIdMessageHeader = resource.idElement.idPart}
                        is Bundle -> { originalIdBundle = resource.idElement.idPart}
                        is Encounter -> { originalIdEncounter = resource.idElement.idPart}
                        is Procedure -> { originalIdProcedure = resource.idElement.idPart}
                        else -> {
                            Assertions.fail("unexpected resource type ${entry.resource.resourceType}")
                        }
                    }
                }
                Assertions.assertNotNull(originalIdPatient)
                Assertions.assertNotNull(originalIdMessageHeader)
                Assertions.assertNotNull(originalIdBundle)
                Assertions.assertNotNull(originalIdEncounter)
                Assertions.assertNotNull(originalIdProcedure)
            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle")
            }
        }

        ourLog.info("**** start rebuild ****")
        // trigger rebuild (operation)
        // Create the input parameters to pass to the server
        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType("rebuildonepdr")

        testClient
            .operation()
            .onServer()
            .named("\$rebuild-account")
            .withParameters(inParams)
            .execute()

        // check other resources
        val patientEverythingResultPostRebuilt : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", patientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResultPostRebuilt.parameter.size)
        when (val everythingBundle = patientEverythingResultPostRebuilt.parameter[0].resource) {
            is Bundle -> {
                // 3 entries stored from the bundle
                Assertions.assertEquals(5, everythingBundle.entry.size)
                var checkedPatient = false
                var checkedMessageHeader = false
                var checkedBundle = false
                var checkedEncounter = false
                var checkedProcedure = false
                everythingBundle.entry.forEach { entry ->
                    when (val resource = entry.resource) {
                        is Patient -> {
                            Assertions.assertEquals(originalIdPatient, resource.idElement.idPart)
                            checkedPatient = true
                        }
                        is MessageHeader -> {
                            Assertions.assertEquals(originalIdMessageHeader, resource.idElement.idPart)
                            checkedMessageHeader = true
                        }
                        is Bundle -> {
                            Assertions.assertEquals(originalIdBundle, resource.idElement.idPart)
                            checkedBundle = true
                        }
                        is Encounter -> {
                            Assertions.assertNotEquals(originalIdEncounter, resource.idElement.idPart)
                            checkedEncounter = true
                        }
                        is Procedure -> {
                            Assertions.assertNotEquals(originalIdProcedure, resource.idElement.idPart)
                            checkedProcedure = true
                        }
                        else -> {
                            Assertions.fail("unexpected resource type ${entry.resource.resourceType}")
                        }
                    }
                }
                Assertions.assertTrue(checkedPatient && checkedMessageHeader && checkedBundle && checkedEncounter && checkedProcedure)
            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle")
            }
        }
    }
}

