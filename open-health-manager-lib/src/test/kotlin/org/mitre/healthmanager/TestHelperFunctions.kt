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
package org.mitre.healthmanager

import ca.uhn.fhir.rest.client.api.IGenericClient
import org.apache.commons.io.IOUtils
import org.awaitility.Awaitility
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Assertions
import org.springframework.core.io.DefaultResourceLoader
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

fun searchForPatientByUsername (username: String, client: IGenericClient, waitSeconds : Long) : String? {
    var results : Bundle? = null
    Awaitility.await().atMost(waitSeconds, TimeUnit.SECONDS).until {
        Thread.sleep(5000) // execute below function every 5 seconds
        results = client
            .search<IBaseBundle>()
            .forResource(Patient::class.java)
            .where(Patient.IDENTIFIER.exactly().systemAndIdentifier("urn:mitre:healthmanager:account:username", username))
            .returnBundle(Bundle::class.java)
            .execute()
        results?.entry?.size!! > 0
    }
    if (results is Bundle) {
        if ((results as Bundle).entry.size == 1) {
            when (val resource = (results as Bundle).entry[0].resource) {
                is Patient -> {
                    return resource.idElement.idPart
                }
                else -> {
                    Assertions.fail("search did not return a patient")
                }
            }
        }
        else {
            Assertions.fail("multiple patients for username $username")
        }
    }
    else {
        Assertions.fail("no results for username $username")
    }
    return null
}

fun stringFromResource(theLocation : String) : String {
    val inputStream : InputStream = if (theLocation.startsWith(File.separator)) {
        FileInputStream(theLocation)
    }
    else {
        val resourceLoader = DefaultResourceLoader()
        val resource = resourceLoader.getResource(theLocation)
        resource.inputStream
    }

    return IOUtils.toString(inputStream, com.google.common.base.Charsets.UTF_8)
}