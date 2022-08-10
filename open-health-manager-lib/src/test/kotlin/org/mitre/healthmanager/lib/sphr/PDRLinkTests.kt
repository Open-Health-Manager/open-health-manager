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
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.mitre.healthmanager.lib.dataMgr.pdrLinkExtensionURL
import org.mitre.healthmanager.lib.dataMgr.pdrLinkListExtensionURL
import org.mitre.healthmanager.lib.dataMgr.usernameSystem
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
class PDRLinkTests {

    private val ourLog = LoggerFactory.getLogger(PDRLinkTests::class.java)
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
    fun testPDRLinksAddedToDirectWrite() {
        val methodName = "testPDRLinksAddedToDirectWrite"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testPDRLinksAddedToDirectWrite"
        val testPatientId = "test-PDRLinksAddedToDirectWrite"
        val testObsId = "test-PDRLinksAddedToDirectWrite-Obs"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.id = testPatientId
        createPatient.meta.source = "urn:mitre:healthmanager:test:source1"

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

        // Observation v1
        val observationV1 = Observation()
        observationV1.subject = Reference().setReference("Patient/$testPatientId")
        observationV1.meta.source = "urn:mitre:healthmanager:test:soruce2"
        observationV1.value = Quantity(50)
        observationV1.code.text = "dummy"
        observationV1.id = testObsId

        val outcomeObsV1: MethodOutcome? = try {
            testClient.update()
                .resource(observationV1)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcomeObsV1)
        Assertions.assertTrue(outcomeObsV1!!.resource is Observation)
        Assertions.assertTrue((outcomeObsV1.resource as Observation).meta.hasExtension(pdrLinkListExtensionURL))
        Assertions.assertEquals(1,(outcomeObsV1.resource as Observation).meta.getExtensionByUrl(pdrLinkListExtensionURL).extension.size)
        Assertions.assertEquals(testObsId,outcomeObsV1.resource.idElement.idPart)

        // Observation v2
        val observationV2 = Observation()
        observationV2.subject = Reference().setReference("Patient/$testPatientId")
        observationV2.meta.source = "urn:mitre:healthmanager:test:soruce3"
        observationV2.value = Quantity(60)
        observationV2.code.text = "dummy"
        observationV2.id = testObsId

        val outcomeObsV2: MethodOutcome? = try {
            testClient.update()
                .resource(observationV2)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcomeObsV2)
        Assertions.assertTrue(outcomeObsV2!!.resource is Observation)
        Assertions.assertTrue((outcomeObsV2.resource as Observation).meta.hasExtension(pdrLinkListExtensionURL))
        Assertions.assertEquals(2,(outcomeObsV2.resource as Observation).meta.getExtensionByUrl(pdrLinkListExtensionURL).extension.size)
        Assertions.assertEquals(testObsId,outcomeObsV2.resource.idElement.idPart)

        // validate Observation exists
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", patientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        var latestPDRBundleId : String? = null
        var earliestPDRBundleId : String? = null
        var observationStored : Observation? = null
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(8, everythingBundle.entry.size)
                var foundObservation = false
                for (entry in everythingBundle.entry) {
                    if (entry.resource is Observation) {
                        Assertions.assertFalse(foundObservation)
                        Assertions.assertEquals(60, (entry.resource as Observation).valueQuantity.value.toInt())
                        foundObservation = true
                        observationStored = (entry.resource as Observation)
                    }
                    else if ((entry.resource is Bundle) &&
                        ((entry.resource as Bundle).entry[0].resource is MessageHeader)) {
                        if (((entry.resource as Bundle).entry[0].resource as MessageHeader).source.endpoint == "urn:mitre:healthmanager:test:soruce3")
                        {
                            latestPDRBundleId = entry.resource.idElement.idPart
                        }
                        else if (((entry.resource as Bundle).entry[0].resource as MessageHeader).source.endpoint == "urn:mitre:healthmanager:test:soruce2")
                        {
                            earliestPDRBundleId = entry.resource.idElement.idPart
                        }
                    }
                }
                Assertions.assertTrue(foundObservation)
                Assertions.assertNotNull(earliestPDRBundleId)
                Assertions.assertNotNull(latestPDRBundleId)
                Assertions.assertNotNull(observationStored)
            }
        }

        // check pdr list
        Assertions.assertTrue(observationStored!!.meta.hasExtension(pdrLinkListExtensionURL))
        val pdrListExtension = observationStored.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertTrue(pdrListExtension.hasExtension(pdrLinkExtensionURL))
        val pdrLinkList = pdrListExtension.getExtensionsByUrl(pdrLinkExtensionURL)
        Assertions.assertEquals(2, pdrLinkList.size)
        Assertions.assertTrue(pdrLinkList[0].hasValue())
        Assertions.assertTrue(pdrLinkList[0].value is Reference)
        Assertions.assertEquals("Bundle/$earliestPDRBundleId", (pdrLinkList[0].value as Reference).reference )
        Assertions.assertTrue(pdrLinkList[1].hasValue())
        Assertions.assertTrue(pdrLinkList[1].value is Reference)
        Assertions.assertEquals("Bundle/$latestPDRBundleId", (pdrLinkList[1].value as Reference).reference )

    }

    @Test
    fun testPDRLinksAddedStoreViaPDR() {
        val methodName = "testPDRLinksAddedStoreViaPDR"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testPDRLinksAddedStoreViaPDR"
        val testPatientId = "test-PDRLinksAddedStoreViaPDR"
        val testObsId = "test-PDRLinksAddedStoreViaPDR-Obs"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.id = testPatientId
        createPatient.meta.source = "urn:mitre:healthmanager:test:source1"

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

        // file PDR bundle
        val messageBundleV1: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/PDRLinkTests/PDRLinksAddedStoreViaPDR_PDR_v1.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundleV1)
            .synchronous(Bundle::class.java)
            .execute()

        // file PDR bundle
        val messageBundleV2: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/PDRLinkTests/PDRLinksAddedStoreViaPDR_PDR_v2.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundleV2)
            .synchronous(Bundle::class.java)
            .execute()

        // validate Observation exists
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", patientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        var latestPDRBundleId : String? = null
        var earliestPDRBundleId : String? = null
        var observationStored : Observation? = null
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(8, everythingBundle.entry.size)
                var foundObservation = false
                for (entry in everythingBundle.entry) {
                    if (entry.resource is Observation) {
                        Assertions.assertFalse(foundObservation)
                        Assertions.assertEquals(21, (entry.resource as Observation).valueQuantity.value.toInt())
                        foundObservation = true
                        observationStored = (entry.resource as Observation)
                    }
                    else if ((entry.resource is Bundle) &&
                        ((entry.resource as Bundle).entry[0].resource is MessageHeader)) {
                        if (((entry.resource as Bundle).entry[0].resource as MessageHeader).source.endpoint == "urn:mitre:healthmanager:test:soruce3")
                        {
                            latestPDRBundleId = entry.resource.idElement.idPart
                        }
                        else if (((entry.resource as Bundle).entry[0].resource as MessageHeader).source.endpoint == "urn:mitre:healthmanager:test:soruce2")
                        {
                            earliestPDRBundleId = entry.resource.idElement.idPart
                        }
                    }
                }
                Assertions.assertTrue(foundObservation)
                Assertions.assertNotNull(earliestPDRBundleId)
                Assertions.assertNotNull(latestPDRBundleId)
                Assertions.assertNotNull(observationStored)
            }
        }

        // check pdr list
        Assertions.assertTrue(observationStored!!.meta.hasExtension(pdrLinkListExtensionURL))
        val pdrListExtension = observationStored.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        Assertions.assertTrue(pdrListExtension.hasExtension(pdrLinkExtensionURL))
        val pdrLinkList = pdrListExtension.getExtensionsByUrl(pdrLinkExtensionURL)
        Assertions.assertEquals(2, pdrLinkList.size)
        Assertions.assertTrue(pdrLinkList[0].hasValue())
        Assertions.assertTrue(pdrLinkList[0].value is Reference)
        Assertions.assertEquals("Bundle/$earliestPDRBundleId", (pdrLinkList[0].value as Reference).reference )
        Assertions.assertTrue(pdrLinkList[1].hasValue())
        Assertions.assertTrue(pdrLinkList[1].value is Reference)
        Assertions.assertEquals("Bundle/$latestPDRBundleId", (pdrLinkList[1].value as Reference).reference )

    }

    @Test
    fun testNoPDRLinksForSharedResourcesStoreViaPDR() {
        val methodName = "testNoPDRLinksForSharedResourcesStoreViaPDR"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testNoPDRLinksForSharedResourcesStoreViaPDR"
        val testPatientId = "test-NoPDRLinksForSharedResourcesStoreViaPDR"
        val testPractitionerId = "test-NoPDRLinksForSharedResourcesStoreViaPDR-prov"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.id = testPatientId
        createPatient.meta.source = "urn:mitre:healthmanager:test:source1"

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

        // file PDR bundle
        val messageBundleV1: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/PDRLinkTests/NoPDRLinksForSharedResourcesStoreViaPDR_PDR.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundleV1)
            .synchronous(Bundle::class.java)
            .execute()

        // validate Practitioner exists
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", testPatientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(6, everythingBundle.entry.size)
                var foundPractitioner = false
                for (entry in everythingBundle.entry) {
                    if (entry.resource is Practitioner) {
                        Assertions.assertFalse(foundPractitioner)
                        Assertions.assertFalse((entry.resource as Practitioner).hasExtension(pdrLinkListExtensionURL))
                        foundPractitioner = true
                    }

                }
                Assertions.assertTrue(foundPractitioner)
            }
        }
    }

    @Test
    fun testNoPDRLinksForSharedResourcesDirectWrite() {
        val methodName = "testNoPDRLinksForSharedResourcesDirectWrite"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testPractitionerId = "test-NoPDRLinksForSharedResourcesDirectWrite-prov"

        val createProv = Practitioner()
        createProv.id = testPractitionerId
        createProv.addName(HumanName().addGiven("test").setFamily("NoPDRLinksForSharedResourcesDirectWrite"))

        val outcome: MethodOutcome? = try {
            testClient.update()
                .resource(createProv)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("create failed")
            null
        }
        Assertions.assertNotNull(outcome)

        val theResource = outcome!!.resource
        if (theResource is Practitioner) {
            Assertions.assertEquals(testPractitionerId, theResource.idElement.idPart)
            Assertions.assertFalse(theResource.meta.hasExtension(pdrLinkListExtensionURL))
        }
    }

    @Test
    fun testPDRLinksForPatientStoreViaPDR() {
        val methodName = "testPDRLinksForPatientStoreViaPDR"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testPDRLinksForPatientStoreViaPDR"
        val testPatientId = "test-PDRLinksForPatientStoreViaPDR"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.id = testPatientId
        createPatient.meta.source = "urn:mitre:healthmanager:test:source1"

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

        // file PDR bundle
        val messageBundleV1: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/PDRLinkTests/PDRLinksForPatientStoreViaPDR_PDR.json")
        )
        testClient
            .operation()
            .processMessage()
            .setMessageBundle<Bundle>(messageBundleV1)
            .synchronous(Bundle::class.java)
            .execute()

        // validate Patient details
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", testPatientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(5, everythingBundle.entry.size)
                var foundPatient = false
                for (entry in everythingBundle.entry) {
                    if (entry.resource is Patient) {
                        Assertions.assertFalse(foundPatient)
                        Assertions.assertTrue((entry.resource as Patient).meta.hasExtension(pdrLinkListExtensionURL))
                        Assertions.assertEquals(2, (entry.resource as Patient).meta.getExtensionByUrl(pdrLinkListExtensionURL).getExtensionsByUrl(
                            pdrLinkExtensionURL).size)
                        foundPatient = true
                    }
                }
                Assertions.assertTrue(foundPatient)
            }
        }
    }

    @Test
    fun testPDRLinksForPatientDirectWrite() {
        val methodName = "testPDRLinksForPatientDirectWrite"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testPDRLinksForPatientDirectWrite"
        val testPatientId = "test-PDRLinksForPatientDirectWrite"

        val createPatient = Patient()
        createPatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        createPatient.id = testPatientId
        createPatient.meta.source = "urn:mitre:healthmanager:test:source1"

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

        val updatePatient = Patient()
        updatePatient.addIdentifier().setSystem(usernameSystem).setValue(testUsername)
        updatePatient.id = testPatientId
        updatePatient.meta.source = "urn:mitre:healthmanager:test:source2"
        updatePatient.addName(HumanName().setFamily("testPDRLinksForPatientDirectWrite").addGiven("Test"))

        val outcome2: MethodOutcome? = try {
            testClient.update()
                .resource(updatePatient)
                .execute();
        } catch (e: Exception) {
            Assertions.fail<String>("update failed")
            null
        }
        Assertions.assertNotNull(outcome2)

        // validate Patient details
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", testPatientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(5, everythingBundle.entry.size)
                var foundPatient = false
                for (entry in everythingBundle.entry) {
                    if (entry.resource is Patient) {
                        Assertions.assertFalse(foundPatient)
                        Assertions.assertTrue((entry.resource as Patient).meta.hasExtension(pdrLinkListExtensionURL))
                        Assertions.assertEquals(2, (entry.resource as Patient).meta.getExtensionByUrl(pdrLinkListExtensionURL).getExtensionsByUrl(
                            pdrLinkExtensionURL).size)
                        foundPatient = true
                    }
                }
                Assertions.assertTrue(foundPatient)
            }
        }
    }

}

