package org.mitre.healthmanager.lib.pdr;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway(errorChannel = "errorChannel")
public interface PdrGateway {

	@Gateway(requestChannel = "processMessageChannel", replyTimeout=1000)
	public IBaseBundle processMessage(IBaseBundle theMessage, @Header("fhirServerBase") String fhirServerBase);

}
