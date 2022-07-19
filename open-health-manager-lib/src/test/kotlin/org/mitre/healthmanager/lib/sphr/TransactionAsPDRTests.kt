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
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mitre.healthmanager.lib.dataMgr.getSourceFromMessageHeader
import org.mitre.healthmanager.lib.dataMgr.pdrLinkExtensionURL
import org.mitre.healthmanager.lib.dataMgr.pdrLinkListExtensionURL
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
class TransactionAsPDRTests {

    private val ourLog = LoggerFactory.getLogger(TransactionAsPDRTests::class.java)
    private val ourCtx: FhirContext = FhirContext.forR4()
    init {
        ourCtx.restfulClientFactory.serverValidationMode = ServerValidationModeEnum.NEVER
        ourCtx.restfulClientFactory.socketTimeout = 1200 * 1000
    }

    @LocalServerPort
    private var port = 0

    @Test
    fun testTransactionAsPDR() {
        val methodName = "testTransactionAsPDR"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testTransactionAsPDR"
        val testPatientId = "test-testTransactionAsPDR"
        val tx1Source = "urn:mitre:healthmanager:test:source1"
        val tx2Source = "urn:mitre:healthmanager:test:source2"

        // file test data
        val transactionBundle1: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/TransactionAsPDRTests/testTransactionAsPDR_Tx1.json")
        )
        val transactionBundle2: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/TransactionAsPDRTests/testTransactionAsPDR_Tx2.json")
        )

        val outcome1: Bundle? = try {
            testClient.transaction().withBundle(transactionBundle1).execute()
        } catch (e: Exception) {
            Assertions.fail<String>("tx1 failed")
            null
        }
        Assertions.assertNotNull(outcome1)
        val outcome2: Bundle? = try {
            testClient.transaction().withBundle(transactionBundle2).execute()
        } catch (e: Exception) {
            Assertions.fail<String>("tx2 failed")
            null
        }
        Assertions.assertNotNull(outcome2)

        ourLog.info("**** get patient id for username ****")
        // give indexing a few more seconds
        val storedPatientId = searchForPatientByUsername(testUsername, testClient, 120)
        Assertions.assertEquals(testPatientId, storedPatientId)

        // validate storage
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", storedPatientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                Assertions.assertEquals(7, everythingBundle.entry.size)

                // loop 1, find bundle ids
                var tx1BundleId : String? = null
                var tx2BundleId : String? = null
                for (entry in everythingBundle.entry) {
                    when (val theResource = entry.resource) {

                        is Bundle -> {
                            when (getSourceFromMessageHeader(getMessageHeader(theResource))) {
                                tx1Source -> {
                                    tx1BundleId = theResource.idElement.idPart
                                }
                                tx2Source -> {
                                    tx2BundleId = theResource.idElement.idPart

                                }
                            }
                        }

                    }

                }
                Assertions.assertNotNull(tx1BundleId)
                Assertions.assertNotNull(tx2BundleId)

                // loop 2 : check links from resources to bundles
                var encounterId : String? = null
                var encounterVersion : String?  = null
                var patientId : String?  = null
                var patientVersion : String?  = null
                var observationId : String?  = null
                var observationVersion : String?  = null
                var bundleCount = 0
                var messageHeaderCount = 0

                for (entry in everythingBundle.entry) {
                    when (val theResource = entry.resource ) {
                        is Encounter -> {
                            encounterId = theResource.idElement.idPart
                            encounterVersion = theResource.meta.versionId

                            // verify link to Tx1
                            Assertions.assertTrue(theResource.meta.hasExtension(
                                pdrLinkListExtensionURL
                            ))
                            Assertions.assertEquals(1,theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL).size)
                            Assertions.assertTrue(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value is Reference)
                            Assertions.assertEquals("Bundle/$tx1BundleId",(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value as Reference).reference)
                        }
                        is Observation -> {
                            observationId = theResource.idElement.idPart
                            observationVersion = theResource.meta.versionId

                            // verify link to Tx2
                            Assertions.assertTrue(theResource.meta.hasExtension(
                                pdrLinkListExtensionURL
                            ))
                            Assertions.assertEquals(1,theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL).size)
                            Assertions.assertTrue(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value is Reference)
                            Assertions.assertEquals("Bundle/$tx2BundleId",(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value as Reference).reference)
                        }
                        is Patient -> {
                            patientId = theResource.idElement.idPart
                            patientVersion = theResource.meta.versionId
                            Assertions.assertEquals("testTransactionAsPDR", theResource.nameFirstRep.family)
                            Assertions.assertEquals(2, theResource.nameFirstRep.given.size)

                            // verify link to Tx1 and Tx2
                            Assertions.assertTrue(theResource.meta.hasExtension(
                                pdrLinkListExtensionURL
                            ))
                            Assertions.assertEquals(2,theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL).size)
                            Assertions.assertTrue(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value is Reference)
                            Assertions.assertEquals("Bundle/$tx1BundleId",(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[0].value as Reference).reference)
                            Assertions.assertTrue(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[1].value is Reference)
                            Assertions.assertEquals("Bundle/$tx2BundleId",(theResource.meta.getExtensionByUrl(
                                pdrLinkListExtensionURL
                            ).getExtensionsByUrl(pdrLinkExtensionURL)[1].value as Reference).reference)
                        }
                        is Bundle -> {
                            bundleCount += 1
                        }
                        is MessageHeader -> {
                            messageHeaderCount += 1
                            when (getSourceFromMessageHeader(theResource)) {
                                tx1Source -> {
                                    Assertions.assertEquals("Bundle/$tx1BundleId", theResource.focus.find{ ref -> ref.reference.startsWith("Bundle")}?.reference)
                                }
                                tx2Source -> {
                                    Assertions.assertEquals("Bundle/$tx2BundleId", theResource.focus.find{ ref -> ref.reference.startsWith("Bundle")}?.reference)
                                }
                            }
                        }
                    }
                }
                Assertions.assertEquals(testPatientId, patientId)
                Assertions.assertEquals("3", patientVersion)
                Assertions.assertNotNull(encounterId)
                Assertions.assertEquals("1", encounterVersion)
                Assertions.assertNotNull(observationId)
                Assertions.assertEquals("1", observationVersion)
                Assertions.assertEquals(2, bundleCount)
                Assertions.assertEquals(2, messageHeaderCount)

                // loop 3 check links from bundles back to resources
                for (entry in everythingBundle.entry) {
                    when (val theResource = entry.resource) {

                        is Bundle -> {
                            when (getSourceFromMessageHeader(getMessageHeader(theResource))) {
                                tx1Source -> {
                                    for (bundleEntry in theResource.entry) {
                                        when (bundleEntry.resource) {
                                            is Patient -> {
                                                Assertions.assertEquals(1, bundleEntry.link.size)
                                                // tx 1 linked to prior version of patient
                                                Assertions.assertEquals("Patient/$patientId/_history/2", bundleEntry.link[0].url)
                                            }
                                            is Encounter -> {
                                                Assertions.assertEquals(1, bundleEntry.link.size)
                                                Assertions.assertEquals("Encounter/$encounterId/_history/$encounterVersion", bundleEntry.link[0].url)

                                            }
                                            is MessageHeader -> {
                                                // expected but no checks - link not stored here
                                            }
                                            else -> {
                                                Assertions.fail("Unexpected resource type in stored tx bundle 1")
                                            }
                                        }
                                    }
                                }
                                tx2Source -> {
                                    for (bundleEntry in theResource.entry) {
                                        when (bundleEntry.resource) {
                                            is Patient -> {
                                                Assertions.assertEquals(1, bundleEntry.link.size)
                                                // tx 2 linked current version of patient
                                                Assertions.assertEquals("Patient/$patientId/_history/$patientVersion", bundleEntry.link[0].url)
                                            }
                                            is Observation -> {
                                                Assertions.assertEquals(1, bundleEntry.link.size)
                                                Assertions.assertEquals("Observation/$observationId/_history/$observationVersion", bundleEntry.link[0].url)

                                            }
                                            is MessageHeader -> {
                                                // expected but no checks - link not stored here
                                            }
                                            else -> {
                                                Assertions.fail("Unexpected resource type in stored tx bundle 1")
                                            }
                                        }
                                    }

                                }
                            }
                        }

                    }

                }


            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle!")
            }
        }



    }

    @Test
    fun testUpdatePatientLinksInTx() {
        val methodName = "testUpdatePatientLinksInTx"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = getAdminAuthClient(ourCtx, "http://localhost:$port/fhir/")
        val testUsername = "testUpdatePatientLinksInTx"

        val inParams = Parameters()
        inParams.addParameter().setName("username").value = StringType(testUsername)
        testClient
            .operation()
            .onServer()
            .named("\$create-account")
            .withParameters(inParams)
            .execute()
        // file test data
        val transactionBundle: Bundle = ourCtx.newJsonParser().parseResource(
            Bundle::class.java, stringFromResource("healthmanager/sphr/TransactionAsPDRTests/testUpdatePatientLinksInTx_Tx.json")
        )

        val outcome: Bundle? = try {
            testClient.transaction().withBundle(transactionBundle).execute()
        } catch (e: Exception) {
            Assertions.fail<String>("tx failed")
            null
        }
        Assertions.assertNotNull(outcome)

        ourLog.info("**** get patient id for username ****")
        // give indexing a few more seconds
        val storedPatientId = searchForPatientByUsername(testUsername, testClient, 120)

        // validate storage
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", storedPatientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                Assertions.assertEquals(4, everythingBundle.entry.size)
                var foundEncounter = false
                var foundPatient = false

                for (entry in everythingBundle.entry) {
                    when (entry.resource ) {
                        is Encounter -> {
                            foundEncounter = true
                        }
                        is Patient -> {
                            foundPatient = true
                        }
                        is Bundle -> {

                        }
                        is MessageHeader -> {

                        }
                    }
                }
                Assertions.assertTrue(foundEncounter)
                Assertions.assertTrue(foundPatient)


            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle!")
            }
        }



    }



}

