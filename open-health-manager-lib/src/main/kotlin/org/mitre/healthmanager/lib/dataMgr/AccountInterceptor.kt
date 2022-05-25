package org.mitre.healthmanager.lib.dataMgr

import ca.uhn.fhir.interceptor.api.Hook
import ca.uhn.fhir.interceptor.api.Interceptor
import ca.uhn.fhir.interceptor.api.Pointcut
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Patient


@Interceptor
class AccountInterceptor {

    /** When creating a patient, ensure there is a username  */
    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
    fun insert(theResource: IBaseResource) {
        if (theResource is Patient) {
            // check that there is an account user name in the identifier list
            getUsernameFromPatient(theResource) ?: throw InternalErrorException("cannot create a patient record without a username identifier (system 'urn:mitre:healthmanager:account:username')")
        }
    }


    /** When updating a patient, ensure the username is maintained */
    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
    fun update(theOldResource: IBaseResource?, theResource: IBaseResource) {

        if ((theOldResource is Patient) && (theResource is Patient)) {
            val oldUsername = getUsernameFromPatient(theOldResource) ?: throw InternalErrorException("update target patient ${theOldResource.idElement.idPart} has no username identifier (system 'urn:mitre:healthmanager:account:username')")
            val updateUsername = getUsernameFromPatient(theResource)
            if (updateUsername == null) {
                addUsernameToPatient(theResource, oldUsername)
            }
            else if (updateUsername != oldUsername) {
                throw InternalErrorException("cannot convert patient instance ${theOldResource.idElement.idPart} from username '$oldUsername' to '$updateUsername'")
            }

        }


    }
}