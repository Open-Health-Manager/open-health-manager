package org.mitre.healthmanager.lib.pdr;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class ProcessMessageService {

	public static final String pdrEvent = "urn:mitre:healthmanager:pdr";
	public static final String pdrAccountExtension = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/AccountExtension";
	public static final String pdrLinkExtensionURL = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/PDRLinkExtension";
	public static final String pdrLinkListExtensionURL = "https://github.com/Open-Health-Manager/patient-data-receipt-ig/StructureDefinition/PDRLinkListExtension";

	public static final MessageHeader getMessageHeader(Bundle theMessage) {
		if (theMessage.getType() != BundleType.MESSAGE) {
			throw new UnprocessableEntityException("$process-message bundle must have type 'message'");
		}

		if (theMessage.getEntry().size() > 0) {
			Resource firstEntry = theMessage.getEntry().get(0).getResource();
			if (firstEntry instanceof MessageHeader) {
				return (MessageHeader) firstEntry;
			} else {
				throw new UnprocessableEntityException(
						"First entry of the message Bundle must be a MessageHeader instance");
			}
		} else {
			throw new UnprocessableEntityException("message Bundle must have at least a MessageHeader entry");
		}
	}

	public static final boolean isPDRMessage(MessageHeader header) {
		Type headerEvent = header.getEvent();
		return headerEvent instanceof UriType
				? ((UriType) headerEvent).getValueAsString().equalsIgnoreCase("urn:mitre:healthmanager:pdr")
				: false;
	}
}
