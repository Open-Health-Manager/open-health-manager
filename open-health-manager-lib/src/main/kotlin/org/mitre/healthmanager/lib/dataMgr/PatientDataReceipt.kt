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

import ca.uhn.fhir.jpa.api.dao.DaoRegistry
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient
import ca.uhn.fhir.jpa.dao.TransactionProcessor
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap
import ca.uhn.fhir.rest.api.SortOrderEnum
import ca.uhn.fhir.rest.api.SortSpec
import ca.uhn.fhir.rest.param.DateRangeParam
import ca.uhn.fhir.rest.param.ReferenceParam
import ca.uhn.fhir.rest.param.UriParam
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Bundle.BundleType
import org.mitre.healthmanager.lib.dataMgr.resourceTypes.doCreate
import org.mitre.healthmanager.lib.dataMgr.resourceTypes.doUpdate
import org.mitre.healthmanager.lib.dataMgr.resourceTypes.findExistingResource
import org.mitre.healthmanager.lib.dataMgr.resourceTypes.isSharedResource
import org.mitre.healthmanager.lib.sphr.getMessageHeader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


const val pdrEvent = "urn:mitre:healthmanager:pdr"
const val pdrAccountExtension = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/AccountExtension"

fun isPDRMessage(header : MessageHeader) : Boolean {
    /// check header event
    return when (val headerEvent = header.event) {
        is UriType -> {
            headerEvent.valueAsString == pdrEvent
        }
        else -> {
            false
        }
    }
}

fun processPDR(header : MessageHeader, theMessage : Bundle, patientDao : IFhirResourceDaoPatient<Patient>, bundleDao : IFhirResourceDao<Bundle>, messageHeaderDao : IFhirResourceDao<MessageHeader>, myTransactionProcessor: TransactionProcessor, doaRegistry: DaoRegistry) {

    // validation: must have at least two entries (header plus content)
    // validation: username extension must be present
    validatePDR(theMessage)
    val username = getUsernameFromPDRHeader(header)

    // identify internal account representation (create if needed
    val patientInternalId = ensureUsername(username, patientDao)

    // store
    // 1. the Bundle in its raw form
    // 2. the MessageHeader linking the Bundle instance to the account Patient instance
    // 3. the non-MessageHeader contents of the Bundle individually
    val bundleInternalId = storePDRAsRawBundle(theMessage, bundleDao)
    val messageHeaderInternalId = storePDRMessageHeader(header.copy(), patientInternalId, bundleInternalId, messageHeaderDao)
    theMessage.entryFirstRep.link.add(Bundle.BundleLinkComponent().setUrl("MessageHeader/$messageHeaderInternalId"))
    storeIndividualPDREntries(theMessage, patientInternalId, myTransactionProcessor, header, username, doaRegistry)
    updatePDRRawBundle(theMessage, bundleDao) // links added
}

fun storePDRAsRawBundle(theMessage: Bundle, bundleDao : IFhirResourceDao<Bundle>) : String {

    val outcome = bundleDao.create(theMessage)
    return outcome.resource.idElement.idPart
}

fun updatePDRRawBundle(theMessage: Bundle, bundleDao : IFhirResourceDao<Bundle>) : String {

    val outcome = bundleDao.update(theMessage)
    return outcome.resource.idElement.idPart
}

fun storePDRMessageHeader(theHeader : MessageHeader, patientInternalId : String, bundleInternalId : String, messageHeaderDao : IFhirResourceDao<MessageHeader>) : String {

    // Store the MessageHeader (theHeader) as its own instance with the following changes
    // The focus list of the message header will be updated to contain only references to
    // - The bundle instance containing the message contents (id via results.id.idPart)
    // - The patient instance representing the account (id via patientInternalId)
    theHeader.focus.clear() // clear existing references for now to avoid need to adjust them later
    theHeader.focus.add(0, Reference("Bundle/$bundleInternalId"))
    theHeader.focus.add(1, Reference("Patient/$patientInternalId"))
    val outcome = messageHeaderDao.create(theHeader)
    return outcome.resource.idElement.idPart
}

fun storeIndividualPDREntries(theMessage: Bundle, patientInternalId: String, myTransactionProcessor: TransactionProcessor, theHeader : MessageHeader?, username : String, doaRegistry: DaoRegistry) {
    val headerToCheck = theHeader ?: getMessageHeader(theMessage)

    if (headerToCheck.source.endpoint == "urn:apple:health-kit") {
        // specific temporary logic to handle apple health kit issues, including
        // 1. no patient entry, which is needed to make the references work
        // 2. links to encounter records, but encounters aren't present
        fixAppleHealthKitBundle(theMessage, patientInternalId)
    }

    // store individual entries
    // take the entries from theMessage and copy into a new bundle with type Transaction, and
    // - remove the first MessageHeader entry
    // - add extensions for links back to the updating PDR
    // - identify an existing entry to update if appropriate
    val accountTxBundle = Bundle()
    accountTxBundle.type = BundleType.TRANSACTION

    //val newEntryToOriginalIndexMap = HashMap<Bundle.BundleEntryComponent, Int>()
    for ((index, entry) in theMessage.entry.withIndex()) {
        if (index == 0) {
            /// skip PDR message header - handled already
            continue
        }
        if (entry.request.method == Bundle.HTTPVerb.DELETE) {
            throw UnprocessableEntityException("Cannot process DELETE as a part of a PDR")
        }

        // create a copy so that the bundle itself remains as sent by the source
        val theResource = entry.resource.copy()
        val bundleEntry = findExistingResource(theResource, patientInternalId)?.let { existingId ->
            // already exists - do an update
            doUpdate(theResource, existingId, username)
        } ?: run {
            // doesn't exist - do a create
            doCreate(theResource, username)
        }

        if ((bundleEntry != null) && (theResource is DomainResource)) {
            if ( !isSharedResource(theResource)) {
                addPatientAccountExtension(theResource, username)
                // add link back to the updating Bundle

                // make sure that we are starting with the latest pdr list
                // this is managed by the system and should not be provided
                // todo: refactor with similar code in RequestInterceptor
                addPDRLinkListExtension(theResource, bundleEntry, doaRegistry, theMessage.idElement.idPart)
            }
            accountTxBundle.addEntry(bundleEntry)
        }
        else {
            throw InternalErrorException("failed to add tracking extensions: bundle entry missing or not a domain resource")
        }
    }

    // process the transaction
    val accountOutcome = myTransactionProcessor.transaction(null, accountTxBundle, false)

    addLinksToStoredPDRBundleBasedOnTxOutcome(accountOutcome, theMessage)
}

fun addLinksToStoredPDRBundleBasedOnTxOutcome(transactionResponse: Bundle, storedPDR: Bundle) {
    for ((index, entry) in transactionResponse.entry.withIndex()) {
        if (entry.response.hasLocation()) {
            val originalEntry = storedPDR.entry[index + 1]
            if (!isSharedResource(originalEntry.resource)) {
                if (entry.response.location.substringBefore("/") != originalEntry.resource.javaClass.simpleName) {
                    throw InternalErrorException("tx response order change")
                }
                originalEntry.link.add(Bundle.BundleLinkComponent().setUrl(entry.response.location))
            }
        } else {
            throw InternalErrorException("didn't get a link back")
        }
    }
}

fun addPDRLinkListExtension(
    theResource: DomainResource,
    bundleEntry: Bundle.BundleEntryComponent,
    doaRegistry: DaoRegistry,
    pdrBundleId: String
) {
    if (theResource.meta.hasExtension(pdrLinkListExtensionURL)) {
        theResource.meta.removeExtension(pdrLinkListExtensionURL)
    }
    if (bundleEntry.request.method == Bundle.HTTPVerb.PUT) {
        val resourceType = theResource.resourceType.toString()
        val targetInternalId = bundleEntry.request.url.substringAfter("/")
        val existingInstance = loadResourceWithTypeAndId(doaRegistry, resourceType, targetInternalId)
        if ((existingInstance is Resource) && (existingInstance.meta.hasExtension(pdrLinkListExtensionURL))) {
            theResource.meta.extension.add(existingInstance.meta.getExtensionByUrl(pdrLinkListExtensionURL))
        }
    }

    addPDRLinkExtension(theResource, pdrBundleId)
}

fun loadResourceWithTypeAndId(
    doaRegistry: DaoRegistry,
    resourceType: String,
    targetInternalId: String
): IBaseResource? {
    val resourceDao = doaRegistry.getResourceDao(resourceType)
        ?: throw InternalErrorException("failed to find the resource Dao for resource type '$resourceType'")
    val existingInstance = try {
        resourceDao.read(IdType(resourceType, targetInternalId))
    } catch (exception: Exception) {
        null
    }
    return existingInstance
}

fun validatePDR(theMessage : Bundle) {
    if (theMessage.entry.size < 2) {
        throw UnprocessableEntityException("Patient Data Receipt must have at least one additional entry beside the MessageHeader")
    }
}

/// Returns the username associated with this messageheader
fun getUsernameFromPDRHeader (header : MessageHeader) : String {

    // get username from extension
    if (header.hasExtension(pdrAccountExtension)) {
        val usernameExtension = header.getExtensionByUrl(pdrAccountExtension)
        when (val usernameExtValue = usernameExtension.value) {
            is StringType -> {
                return usernameExtValue.value
            }
            else -> {
                throw UnprocessableEntityException("invalid username extension in pdr message header")
            }
        }
    }
    else {
        throw UnprocessableEntityException("no username found in pdr message header")
    }

}

fun isGUID(theId : String?) : Boolean {
    return try {
        UUID.fromString(theId)
        true
    } catch (exception: IllegalArgumentException) {
        false
    }
}

const val pdrLinkExtensionURL = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/PDRLinkExtension"
const val pdrLinkListExtensionURL : String = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/PDRLinkListExtension"

fun addPDRLinkExtension(theResource: DomainResource, bundleId : String) {
    val pdrListExtension = getPDRLinkListExtensionFromResource(theResource)
    val newLinkExtension = Extension().setUrl(pdrLinkExtensionURL)
    newLinkExtension.setValue(Reference("Bundle/$bundleId"))
    pdrListExtension.addExtension(newLinkExtension)
}

fun getPDRLinkListExtensionFromResource(theResource: DomainResource) : Extension {
    return theResource.meta.getExtensionByUrl(pdrLinkListExtensionURL)
        ?: run {
            /// Empty list
            val newExtension = Extension().setUrl(pdrLinkListExtensionURL)
            theResource.meta.extension.add(newExtension)
            newExtension
        }
}

fun findRecentPDRForPatientAndSource(patientInternalId: String, source : String, lookbackTimeSeconds : Long, messageHeaderDao: IFhirResourceDao<MessageHeader>) : MessageHeader? {
    val lastUpdatedTimestamp = LocalDateTime.now().minusSeconds(lookbackTimeSeconds).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
    val searchParameterMap = SearchParameterMap()
    searchParameterMap.add(MessageHeader.SP_FOCUS, ReferenceParam("Patient/$patientInternalId"))
    searchParameterMap.add(MessageHeader.SP_SOURCE_URI, UriParam(source))
    searchParameterMap.lastUpdated = DateRangeParam("ge$lastUpdatedTimestamp", null)
    searchParameterMap.sort = SortSpec("_lastUpdated").setOrder(SortOrderEnum.DESC)
    searchParameterMap.isLoadSynchronous = true /// disable cache since we're finding recent stuff
    val searchResults = messageHeaderDao.search(searchParameterMap)
    val messageHeaderResultList: List<IBaseResource> = searchResults.allResources

    return if (messageHeaderResultList.isNotEmpty()) {

        when (val resource = messageHeaderResultList[0]) {
            is MessageHeader -> {
                resource
            }
            else -> { throw InternalErrorException("internal search returned a non-MessageHeader resource")}
        }
    }
    else {
        null
    }
}

fun getSourceFromMessageHeader(messageHeader: MessageHeader) : String {
    return messageHeader.source.endpoint
}

fun generatePDRMessageHeaderObject(username: String, source: String) : MessageHeader {
    val theHeader = MessageHeader()
    theHeader.source = MessageHeader.MessageSourceComponent().setEndpoint(source)
    theHeader.event = UriType(pdrEvent)
    theHeader.addExtension(Extension(pdrAccountExtension, StringType(username)))

    return theHeader
}

fun generatePDRBundleObject(messageHeader: MessageHeader) : Bundle {
    val theMessage = Bundle()
    theMessage.type = BundleType.MESSAGE
    theMessage.entry.add(Bundle.BundleEntryComponent().setResource(messageHeader.copy()))

    return theMessage
}

fun getBundleIdFromMessageHeader(messageHeader: MessageHeader) : String {
    messageHeader.focus.forEach {
        val resourceTypeAndId = it.reference.toString().split("/")
        if (resourceTypeAndId[0] == "Bundle") {
            return resourceTypeAndId[1]
        }
    }

    throw InternalErrorException("no bundle id associated with MessageHeader")
}

fun getPatientIdFromMessageHeader(messageHeader: MessageHeader) : String {
    messageHeader.focus.forEach {
        val resourceTypeAndId = it.reference.toString().split("/")
        if (resourceTypeAndId[0] == "Patient") {
            return resourceTypeAndId[1]
        }
    }

    throw InternalErrorException("no patient id associated with MessageHeader")
}