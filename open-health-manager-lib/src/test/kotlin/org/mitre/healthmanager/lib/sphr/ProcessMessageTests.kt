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
import org.apache.commons.io.IOUtils
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.*
import org.mitre.healthmanager.lib.dataMgr.pdrAccountExtension
import org.mitre.healthmanager.lib.dataMgr.pdrLinkListExtensionURL
import org.mitre.healthmanager.stringFromResource
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.io.DefaultResourceLoader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

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
class ProcessMessageTests {

    private val ourLog = LoggerFactory.getLogger(ProcessMessageTests::class.java)
    private val ourCtx: FhirContext = FhirContext.forR4()
    init {
        ourCtx.restfulClientFactory.serverValidationMode = ServerValidationModeEnum.NEVER
        ourCtx.restfulClientFactory.socketTimeout = 1200 * 1000
    }

    @LocalServerPort
    private var port = 0

    @Test
    fun testSuccessfulBundleStorage() {
        val methodName = "testSuccessfulBundleStorage"
        ourLog.info("Entering $methodName()...")
        val testClient : IGenericClient = ourCtx.newRestfulGenericClient("http://localhost:$port/fhir/")

        // Submit the bundle
        val messageBundle: Bundle = ourCtx.newJsonParser().parseResource<Bundle>(
            Bundle::class.java, stringFromResource("healthmanager/sphr/ProcessMessageTests/BundleMessage_valid.json")
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

        Thread.sleep(1000) // give indexing a second to occur

        // find the patient id
        val patientResultsBundle : Bundle = testClient
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            //.where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", "aKutch271"))
            .returnBundle(Bundle::class.java)
            .execute()

        /* Indexing seems to be taking too long, so don't search on patient id
        // give indexing a few more seconds
        if (patientResultsBundle.entry.size == 0) {
            Awaitility.await().atMost(1, TimeUnit.MINUTES).until {
                Thread.sleep(5000) // execute below function every 1 second
                ourLog.info("waiting for indexing")
                patientResultsBundle = testClient
                    .search<IBaseBundle>()
                    .forResource(Patient::class.java)
                    .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", "aKutch271"))
                    .returnBundle(Bundle::class.java)
                    .execute()
                patientResultsBundle?.entry?.size!! > 0
            }
        }
         */

        Assertions.assertEquals(1, patientResultsBundle.entry.size)
        val patientId = when (val firstResource = patientResultsBundle.entry[0].resource) {
            is Patient -> {
                val username = firstResource.identifier.filter { id -> id.system == "urn:mitre:healthmanager:account:username" }
                Assertions.assertEquals(1, username.size)
                Assertions.assertEquals("aKutch271", username[0].value)
                firstResource.idElement.idPart
            }
            else -> {
                Assertions.fail("response didn't return a patient")
            }
        }

        // look for the message header
        val messageHeaderResults : Bundle = testClient
            .search<IBaseBundle>()
            .forResource(MessageHeader::class.java)
            .returnBundle(Bundle::class.java)
            .execute()
        Assertions.assertEquals(1, messageHeaderResults.entry.size)
        when (val messageHeader = messageHeaderResults.entry[0].resource) {
            is MessageHeader -> {
                var foundPatient = false
                var foundBundle = false
                // 2 focuses: patient and bundle
                Assertions.assertEquals(2, messageHeader.focus.size)
                messageHeader.focus.forEach { aFocus ->
                    when {

                        (aFocus.referenceElement.resourceType == "Patient") -> {
                            Assertions.assertEquals(patientId, aFocus.referenceElement.idPart)
                            foundPatient = true
                        }
                        (aFocus.referenceElement.resourceType == "Bundle") -> {
                            foundBundle = true
                        }
                        else -> {
                            Assertions.fail("unexpected focus")
                        }
                    }
                }
                Assertions.assertTrue(foundBundle)
                Assertions.assertTrue(foundPatient)
            }
            else -> {
                Assertions.fail("not a message header")
            }
        }

        // check other resources
        val patientEverythingResult : Parameters = testClient
            .operation()
            .onInstance(IdType("Patient", patientId))
            .named("\$everything")
            .withNoParameters(Parameters::class.java)
            .useHttpGet()
            .execute()
        Assertions.assertEquals(1, patientEverythingResult.parameter.size)
        when (val everythingBundle = patientEverythingResult.parameter[0].resource) {
            is Bundle -> {
                // 4 entries stored from the bundle
                Assertions.assertEquals(6, everythingBundle.entry.size)
                var foundPatient = false
                var foundMessageHeader = false
                var foundBundle = false
                var foundEncounter = false
                var foundProcedure = false
                var foundPractitioner = false
                everythingBundle.entry.forEach { entry ->
                    when (val theResource = entry.resource) {
                        is Patient -> {
                            foundPatient = true
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrAccountExtension))
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrLinkListExtensionURL))
                        }
                        is MessageHeader -> {
                            foundMessageHeader = true
                        }
                        is Bundle -> {
                            foundBundle = true
                            for (entry in theResource.entry) {
                                when (val entryResource = entry.resource) {
                                    is Patient -> {
                                        Assertions.assertEquals(1, entry.link.size)
                                    }
                                    is Encounter -> {
                                        Assertions.assertEquals(1, entry.link.size)
                                    }
                                    is Procedure -> {
                                        Assertions.assertEquals(1, entry.link.size)
                                    }
                                    is Practitioner -> {
                                        Assertions.assertEquals(0, entry.link.size)
                                    }
                                    is MessageHeader -> {
                                        Assertions.assertEquals(1, entry.link.size)
                                    }
                                    else -> {
                                        Assertions.fail("unexpected entry resource type in bundle")
                                    }
                                }
                            }
                        }
                        is Encounter -> {
                            foundEncounter = true
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrAccountExtension))
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrLinkListExtensionURL))
                        }
                        is Procedure -> {
                            foundProcedure = true
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrAccountExtension))
                            Assertions.assertTrue(theResource.meta.hasExtension(pdrLinkListExtensionURL))
                        }
                        is Practitioner -> {
                            foundPractitioner = true
                            Assertions.assertFalse(theResource.meta.hasExtension(pdrAccountExtension))
                            Assertions.assertFalse(theResource.meta.hasExtension(pdrLinkListExtensionURL))
                        }
                        else -> {
                            Assertions.fail("unexpected resource type ${entry.resource.resourceType}")
                        }
                    }
                }
                Assertions.assertTrue(foundPatient && foundBundle && foundMessageHeader && foundProcedure && foundEncounter && foundPractitioner)
            }
            else -> {
                Assertions.fail("\$everything didn't return a bundle")
            }
        }
    }
}