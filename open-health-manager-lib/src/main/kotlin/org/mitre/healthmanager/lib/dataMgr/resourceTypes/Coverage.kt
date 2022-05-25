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
package org.mitre.healthmanager.lib.dataMgr.resourceTypes

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient
import org.hl7.fhir.r4.model.Coverage
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient

// check the beneficiary for a patient reference, and follow it to get the username
fun Coverage.findUsernameViaLinkedPatient(patientDao : IFhirResourceDaoPatient<Patient>) : String? {
    return getPatientUsernameFromReference(this.beneficiary, patientDao, "Coverage")
}