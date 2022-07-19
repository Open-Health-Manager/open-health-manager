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
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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
class DeleteAccountTests {

    private val ourLog = LoggerFactory.getLogger(DeleteAccountTests::class.java)
    private val ourCtx: FhirContext = FhirContext.forR4()
    init {
        ourCtx.restfulClientFactory.serverValidationMode = ServerValidationModeEnum.NEVER
        ourCtx.restfulClientFactory.socketTimeout = 1200 * 1000
    }

    @LocalServerPort
    private var port = 0

    @Test
    fun testOnePDRDelete() {
        val methodName = "testOnePDRDelete"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")

        // file test data
        // has username identifier and first / last name
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource<Bundle>(
            Bundle::class.java, stringFromResource("healthmanager/dataMgr/RebuildAccountTests/SinglePDRRebuild.json")
        )
        val response : Bundle = testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundle)
            .synchronous(Bundle::class.java)
            .execute()

        ourLog.info("**** get patient id for username ****")
        // give indexing a few more seconds
        val patientId: String? = searchForPatientByUsername("rebuildonepdr", testClient, 120)

        Assertions.assertNotNull(patientId)
        val patResource = testClient.read().resource(Patient::class.java).withId(patientId).encodedJson().execute()
        Assertions.assertEquals("Rebuild", patResource.nameFirstRep.family)
        Assertions.assertEquals("OnePDR", patResource.nameFirstRep.givenAsSingleString)

        // check other resources
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
            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle")
            }
        }

        ourLog.info("**** start delete ****")
        // trigger rebuild (operation)
        // Create the input parameters to pass to the server
        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType("rebuildonepdr")

        testClient
            .operation()
            .onServer()
            .named("\$delete-account")
            .withParameters(inParams)
            .execute()

        val postDeletePatientSearch = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.RES_ID.exactly().code(patientId))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, postDeletePatientSearch.entry.size)
    }
}