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

import ca.uhn.fhir.jpa.api.dao.DaoRegistry
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient
import ca.uhn.fhir.jpa.dao.TransactionProcessor
import ca.uhn.fhir.jpa.dao.r4.FhirSystemDaoR4
import ca.uhn.fhir.jpa.starter.AppProperties
import ca.uhn.fhir.rest.api.server.RequestDetails
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.MessageHeader
import org.hl7.fhir.r4.model.Patient
import org.mitre.healthmanager.lib.dataMgr.isPDRMessage
import org.mitre.healthmanager.lib.dataMgr.processPDR
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier


@Autowired
val appProperties: AppProperties? = null

open class ProcessMessage : FhirSystemDaoR4() {

    @Autowired
    @Qualifier("myPatientDaoR4")
    private lateinit var myPatientDao: IFhirResourceDaoPatient<Patient>

    @Autowired
    @Qualifier("myBundleDaoR4")
    private lateinit var myBundleDao: IFhirResourceDao<Bundle>

    @Autowired
    @Qualifier("myMessageHeaderDaoR4")
    private lateinit var myMessageHeaderDao: IFhirResourceDao<MessageHeader>

    @Autowired
    private lateinit var myTransactionProcessor: TransactionProcessor

    @Autowired
    private lateinit var myDaoRegistry: DaoRegistry

    override fun processMessage(theRequestDetails: RequestDetails, theMessage: IBaseBundle?): IBaseBundle {

        // Validation and initial processing
        // - payload must be a bundle
        if (theMessage !is Bundle) {
            throw UnprocessableEntityException("bundle not provided to \$process-message")
        }
        // - bundle must have type 'message'
        // - bundle first entry must be a MessageHeader entry
        val theHeader = getMessageHeader(theMessage)

        if (isPDRMessage(theHeader) ) {
            processPDR(theHeader, theMessage, myPatientDao, myBundleDao, myMessageHeaderDao, myTransactionProcessor, myDaoRegistry)
        }
        else {
            throw UnprocessableEntityException("message event not supported")
        }

        // NOTE: this line is the reason the provider doesn't do this itself
        // -- it doesn't know its own address (HapiProperties is JPA server only)
        val serverAddress: String = appProperties?.server_address ?: theRequestDetails.fhirServerBase
        val response = Bundle()
        response.type = Bundle.BundleType.MESSAGE
        val newHeader = MessageHeader()
        newHeader.addDestination().endpoint = theHeader.source.endpoint
        newHeader.source = MessageHeader.MessageSourceComponent()
            .setEndpoint("$serverAddress\$process-message")
        newHeader.response = MessageHeader.MessageHeaderResponseComponent()
            .setCode(MessageHeader.ResponseType.OK)
        response.addEntry().resource = newHeader

        return response
    }
}

fun getMessageHeader(theMessage : Bundle) : MessageHeader {

    if (theMessage.type != Bundle.BundleType.MESSAGE) {
        throw UnprocessableEntityException("\$process-message bundle must have type 'message'")
    }

    if (theMessage.entry.size > 0) {
        when (val firstEntry = theMessage.entry[0].resource) {
            is MessageHeader -> {
                return firstEntry
            }
            else -> {
                throw UnprocessableEntityException("First entry of the message Bundle must be a MessageHeader instance")
            }
        }
    }
    else {
        throw UnprocessableEntityException("message Bundle must have at least a MessageHeader entry")
    }

}



