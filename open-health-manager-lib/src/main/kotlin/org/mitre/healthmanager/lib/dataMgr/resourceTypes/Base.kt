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
import ca.uhn.fhir.model.primitive.IdDt
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent
import org.mitre.healthmanager.lib.dataMgr.getUsernameFromPatient
import org.mitre.healthmanager.lib.dataMgr.pdrAccountExtension
import org.mitre.healthmanager.lib.dataMgr.sharedUsername

/// Look for an existing stored entry
/// NOTE: Implemented with extension methods which are statically dispatched,
/// hence the when-based smart-cast dispatch
fun findExistingResource(theResource: Resource, patientInternalId : String) : String? {
    return when (theResource) {
        is Patient -> { theResource.findExistingResource(patientInternalId) }
        else -> { theResource.findExistingResource(patientInternalId) }
    }
}

/// Create a transaction entry to create this resource in the database
/// NOTE: Implemented with extension methods which are statically dispatched,
/// hence the when-based smart-cast dispatch
fun doCreate(theResource: Resource, username: String) : BundleEntryComponent? {
    return when (theResource) {
        is Patient -> { theResource.doCreate(username) }
        else -> { theResource.doCreate(username) }
    }
}

/// Create a transaction entry to update this resource in the database
/// NOTE: Implemented with extension methods which are statically dispatched,
/// hence the when-based smart-cast dispatch
fun doUpdate(theResource: Resource, id: String, username: String) : BundleEntryComponent?  {
    return when (theResource) {
        is Patient -> { theResource.doUpdate(id, username) }
        else -> { theResource.doUpdate(id, username) }
    }
}

/// identify the username by following patient links
/// NOTE: Implemented with extension methods which are statically dispatched,
/// hence the when-based smart-cast dispatch
fun findUsernameViaLinkedPatient(theResource: Resource, patientDao : IFhirResourceDaoPatient<Patient>) : String? {

    return when (theResource) {
        is Patient -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Observation -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Condition -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Encounter -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Procedure -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Bundle -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is MessageHeader -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is CarePlan -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is ServiceRequest -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is QuestionnaireResponse -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is ClinicalImpression -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is EpisodeOfCare -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is RelatedPerson -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Goal -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Coverage -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Claim -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is RiskAssessment -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        is Contract -> { theResource.findUsernameViaLinkedPatient(patientDao) }
        else -> { theResource.findUsernameViaLinkedPatient(patientDao) }
    }
}

/// Is this resource shared (not account-specific)?
fun isSharedResource(theResource: Resource) : Boolean {
    return if (isSharedResourceMap.containsKey(theResource.javaClass.simpleName)) {
        // make sure it doesn't have a specific username extension - remove if needed
        if (theResource.meta.hasExtension(pdrAccountExtension)) {
            theResource.meta.removeExtension(pdrAccountExtension)
        }
        true
    }
    else {
        false
    }
}

fun isSharedResource(resourceType: String) : Boolean {
    return isSharedResourceMap.containsKey(resourceType)
}


val isSharedResourceMap = hashMapOf(
    "Questionnaire" to true,
    "Organization" to true,
    "Practitioner" to true,
    "Location" to true,
    "PractitionerRole" to true,
    "StructureDefinition" to true,
    "SearchParameter" to true
    )

fun getPatientUsernameFromReference(theReference: Reference, patientDao : IFhirResourceDaoPatient<Patient>, resourceType: String) : String? {
    if (theReference.referenceElement.resourceType == "Patient") {
        val patient = try {
            patientDao.read(IdDt(theReference.referenceElement.idPart))
        } catch (exception : ResourceNotFoundException) {
            // must be being created as a part of this transaction, or will fail later
            return null
        }
        getUsernameFromPatient(patient)?.let { username ->
            if (username == sharedUsername) {
                return null
            }
            else {
                return username
            }
        } ?: throw InternalErrorException("$resourceType references a patient with no username")
    }
    else {
        throw UnprocessableEntityException("$resourceType must reference a patient")
    }
}