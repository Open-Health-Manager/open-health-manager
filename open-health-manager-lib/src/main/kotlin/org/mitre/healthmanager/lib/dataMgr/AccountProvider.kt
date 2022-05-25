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

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.jpa.api.dao.DaoRegistry
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDaoPatient
import ca.uhn.fhir.jpa.dao.TransactionProcessor
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.rest.annotation.Operation
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.r4.model.*
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AccountProvider(private val myPatientDaoR4: IFhirResourceDaoPatient<Patient>,
                      private val myBundleDaoR4: IFhirResourceDao<Bundle>,
                      private val myMessageHeaderDaoR4: IFhirResourceDao<MessageHeader>,
                      private val myTransactionProcessor: TransactionProcessor,
                      private val myDaoRegistry: DaoRegistry){

    @Operation(name = "\$rebuild-account", manualResponse = true, manualRequest = true)
    @Throws(
        IOException::class
    )
    fun rebuildAccountOperation(theServletRequest: HttpServletRequest, theServletResponse: HttpServletResponse) {

        val reader = theServletRequest.reader
        val data: String = reader.readText()
        reader.close()

        val ctx = FhirContext.forR4()
        val parser: IParser = ctx.newJsonParser()
        val parsedData: Parameters = parser.parseResource(Parameters::class.java, data)

        val username = when (val usernameRaw = parsedData.parameter[0].value) {
            is StringType -> {
                usernameRaw.value
            }
            else -> {
                throw UnprocessableEntityException("\$rebuild-account parameter must be a string")
            }
        }
        if (username == "") {
            throw UnprocessableEntityException("\$create-account parameter must be non-empty")
        }

        rebuildAccount(username, myPatientDaoR4, myBundleDaoR4, myMessageHeaderDaoR4, myTransactionProcessor, theServletRequest, myDaoRegistry)

        theServletResponse.contentType = "application/fhir+json"
        theServletResponse.writer.write(ctx.newJsonParser().encodeResourceToString(getOkOutcome()))
        theServletResponse.writer.close()
    }

    @Operation(name = "\$delete-account", manualResponse = true, manualRequest = true)
    @Throws(
        IOException::class
    )
    fun deleteAccountOperation(theServletRequest: HttpServletRequest, theServletResponse: HttpServletResponse) {

        val reader = theServletRequest.reader
        val data: String = reader.readText()
        reader.close()

        val ctx = FhirContext.forR4()
        val parser: IParser = ctx.newJsonParser()
        val parsedData: Parameters = parser.parseResource(Parameters::class.java, data)

        val username = when (val usernameRaw = parsedData.parameter[0].value) {
            is StringType -> {
                usernameRaw.value
            }
            else -> {
                throw UnprocessableEntityException("\$delete-account parameter must be a string")
            }
        }
        if (username == "") {
            throw UnprocessableEntityException("\$create-account parameter must be non-empty")
        }

        deleteAccount(username, myPatientDaoR4, myTransactionProcessor, theServletRequest)

        theServletResponse.contentType = "application/fhir+json"
        theServletResponse.writer.write(ctx.newJsonParser().encodeResourceToString(getOkOutcome()))
        theServletResponse.writer.close()
    }

    @Operation(name = "\$create-account", manualResponse = true, manualRequest = true)
    @Throws(
        IOException::class
    )
    fun createAccountOperation(theServletRequest: HttpServletRequest, theServletResponse: HttpServletResponse) {

        val reader = theServletRequest.reader
        val data: String = reader.readText()
        reader.close()

        val ctx = FhirContext.forR4()
        val parser: IParser = ctx.newJsonParser()
        val parsedData: Parameters = parser.parseResource(Parameters::class.java, data)

        val usernameParam = parsedData.parameter.find { parameter -> (parameter.name == "username") } ?: throw UnprocessableEntityException("\$create-account parameter must be non-empty")
        val username = when (val usernameRaw = usernameParam.value) {
            is StringType -> {
                usernameRaw.value
            }
            else -> {
                throw UnprocessableEntityException("\$create-account parameter must be a string")
            }
        }
        if (username == "") {
            throw UnprocessableEntityException("\$create-account parameter must be non-empty")
        }

        val targetPatientId = parsedData.parameter.find { parameter -> (parameter.name == "targetId") }?.let { parameter ->
            when (val rawValue = parameter.value) {
                is StringType -> {
                    rawValue.value
                }
                else -> {
                    null
                }
            }
        }

        createAccount(username, myPatientDaoR4, targetPatientId)

        theServletResponse.contentType = "application/fhir+json"
        theServletResponse.writer.write(ctx.newJsonParser().encodeResourceToString(getOkOutcome()))
        theServletResponse.writer.close()
    }


}

fun getOkOutcome() : OperationOutcome {
    val outcomeOk = OperationOutcome()
    val issueOk = OperationOutcome.OperationOutcomeIssueComponent()
    issueOk.code = OperationOutcome.IssueType.INFORMATIONAL
    issueOk.severity = OperationOutcome.IssueSeverity.INFORMATION
    issueOk.details.text = "All OK"
    outcomeOk.issue.add(issueOk)
    return outcomeOk
}