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

import org.hl7.fhir.r4.model.*

fun fixAppleHealthKitBundle(theMessage : Bundle, internalPatientId : String) {
    var messagePatientId : String? = null

    theMessage.entry.forEach { entry ->
        when (val resource = entry.resource) {
            is Observation -> {

                // replace patient reference with internal reference
                resource.subject.reference = "Patient/$internalPatientId"
                /*
                val patientReference = resource.subject.reference

                val referencedPatientId = patientReference.substringAfter("/")
                if (messagePatientId == null) {
                    messagePatientId = referencedPatientId
                }
                else if (messagePatientId != referencedPatientId) {
                    throw UnprocessableEntityException("Health kit: multiple referenced patients provided, only one allowed")
                }

                 */
                // remove encounter link
                resource.encounter = null

            }
            is Procedure -> {
                // replace patient reference with internal reference
                resource.subject.reference = "Patient/$internalPatientId"
                // remove encounter link
                resource.encounter = null
                resource.performer.clear()
            }
            is Condition -> {
                // replace patient reference with internal reference
                // NOTE: in DSTU-2 it is patient instead of subject, so probably can't get conditions currently
                resource.subject.reference = "Patient/$internalPatientId"
                resource.asserter = null
            }
            is AllergyIntolerance -> {
                // replace patient reference with internal reference
                resource.patient.reference = "Patient/$internalPatientId"
            }
            is Immunization -> {
                // replace patient reference with internal reference
                resource.patient.reference = "Patient/$internalPatientId"
                // remove encounter link
                resource.encounter = null
                resource.performer.clear()
            }
            else -> {
                // do nothing
            }
        }



    }
}