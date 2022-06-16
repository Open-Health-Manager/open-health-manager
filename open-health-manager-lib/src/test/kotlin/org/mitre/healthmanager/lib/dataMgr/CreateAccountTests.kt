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
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Parameters
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mitre.healthmanager.searchForPatientByUsername
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
class CreateAccountTests {

    private val ourLog = LoggerFactory.getLogger(CreateAccountTests::class.java)
    private val ourCtx: FhirContext = FhirContext.forR4()
    init {
        ourCtx.restfulClientFactory.serverValidationMode = ServerValidationModeEnum.NEVER
        ourCtx.restfulClientFactory.socketTimeout = 1200 * 1000
    }

    @LocalServerPort
    private var port = 0

    @Test
    fun testCreateSuccess() {
        val methodName = "testCreateSuccess"
        ourLog.info("Entering $methodName()...")
        val testClient: IGenericClient = ourCtx.newRestfulGenericClient("http://localhost:$port/fhir/")

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", "createNew"))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // trigger create
        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType("createNew")
        testClient
            .operation()
            .onServer()
            .named("\$create-account")
            .withParameters(inParams)
            .execute()

        // make sure patient does exist
        val patientCreatedId: String? = searchForPatientByUsername("createNew", testClient, 120)
        Assertions.assertNotNull(patientCreatedId)

    }

    @Test
    fun testCreateWithSpecificId() {
        val methodName = "testCreateSuccess"
        ourLog.info("Entering $methodName()...")
        val testClient: IGenericClient = ourCtx.newRestfulGenericClient("http://localhost:$port/fhir/")
        val testUsername = "createNewWithId"
        val testPatientId = "test-createNewWithId"

        // make sure patient doesn't exist
        val results = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", testUsername))
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(0, results?.entry?.size!!)

        // trigger create
        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType(testUsername)
        inParams.addParameter().setName("targetId").value = StringType(testPatientId)
        testClient
            .operation()
            .onServer()
            .named("\$create-account")
            .withParameters(inParams)
            .execute()

        // make sure patient does exist with the specified Id
        val patientCreatedId: String? = searchForPatientByUsername(testUsername, testClient, 120)
        Assertions.assertEquals(testPatientId, patientCreatedId)

    }
}