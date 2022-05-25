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
import ca.uhn.fhir.jpa.dao.TransactionProcessor
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.mitre.healthmanager.lib.dataMgr.addUsernameToPatient
import org.mitre.healthmanager.lib.dataMgr.getUsernameFromPatient

/// for patients, get the account username
fun Patient.findExistingResource(patientInternalId: String) : String? {
    return patientInternalId
}

/// make sure the patient has the username
fun Patient.doCreate(username: String) : Bundle.BundleEntryComponent? {
    getUsernameFromPatient(this)?.let {
        if (it != username) {
            throw UnprocessableEntityException("cannot change username associated with a patient")
        }
    }?: run {
        addUsernameToPatient(this, username)
    }

    return (this as Resource).doCreate(
        username
    )
}

/// make sure the patient has the username
fun Patient.doUpdate(id: String, username: String) : Bundle.BundleEntryComponent? {
    getUsernameFromPatient(this)?.let {
        if (it != username) {
            throw UnprocessableEntityException("cannot change username associated with a patient")
        }
    }?: run {
        addUsernameToPatient(this, username)
    }

    return (this as Resource).doUpdate(
        id,
        username
    )
}

// we have the patient, look for the username identifier
// fall back to the database
fun Patient.findUsernameViaLinkedPatient(patientDao : IFhirResourceDaoPatient<Patient>) : String? {

    return getUsernameFromPatient(this) ?: run {
        if (this.id != null) {
            try {
                getUsernameFromPatient(patientDao.read(this.idElement))
            } catch (e : Exception) {
                null
            }

        }
        else {
            null
        }
    }
}