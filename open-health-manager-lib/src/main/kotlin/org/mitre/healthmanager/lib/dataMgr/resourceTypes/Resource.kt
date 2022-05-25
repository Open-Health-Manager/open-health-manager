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
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.mitre.healthmanager.lib.dataMgr.isGUID

/// default implementation:
/// - if an id is present, check if it exists in the system
/// - otherwise, no match
fun Resource.findExistingResource(patientInternalId : String) : String? {
    return if (this.idElement.idPart != null) {
        // todo: implement the check - need the Dao
        null
    }
    else {
        null
    }
}

/// default implementation: wrap in a transaction and store
fun Resource.doCreate( username: String) : Bundle.BundleEntryComponent? {

    val entry = Bundle.BundleEntryComponent()
    val resourceType = this.resourceType.name
    entry.resource = this

    val resourceId = this.idElement.idPart
    entry.fullUrl = when {
        resourceId == null -> {
            "" // can't refer to this resource
        }
        isGUID(resourceId) -> {
            "urn:uuid:$resourceId"
        }
        resourceId != "" -> {
            "${entry.resource.resourceType}/$resourceId"
        }
        else -> {
            "" // can't refer to this resource
        }
    }

    if ((resourceId == null) || (resourceId == "")) {
        entry.request.url = resourceType
        entry.request.method = Bundle.HTTPVerb.POST
    }
    else {
        when (resourceId.toIntOrNull()) {
            null -> {
                // Non-empty, not an int, so can store it
                // todo: should this be an option (will update if already exists)?
                entry.request.url = "$resourceType/$resourceId"
                entry.request.method = Bundle.HTTPVerb.PUT

            }
            else -> {
                // non-empty int, HAPI won't create
                entry.request.url = resourceType
                entry.request.method = Bundle.HTTPVerb.POST
            }
        }
    }
    return entry
}

/// default implementation: wrap in a transaction and store
fun Resource.doUpdate(id: String, username: String) : Bundle.BundleEntryComponent?  {

    val entry = Bundle.BundleEntryComponent()
    val resourceId = this.idElement.idPart
    entry.fullUrl = when {
        resourceId == null -> {
            "" // can't refer to this resource
        }
        isGUID(resourceId) -> {
            "urn:uuid:$resourceId"
        }
        resourceId != "" -> {
            "${this.resourceType}/$resourceId"
        }
        else -> {
            "" // can't refer to this resource
        }
    }

    val resourceType = this.resourceType.name
    entry.resource = this
    entry.request.url = "$resourceType/$id"
    entry.request.method = Bundle.HTTPVerb.PUT

    return entry
}

/// base implementation fails
fun Resource.findUsernameViaLinkedPatient(patientDao : IFhirResourceDaoPatient<Patient>) : String? {

    return null
}