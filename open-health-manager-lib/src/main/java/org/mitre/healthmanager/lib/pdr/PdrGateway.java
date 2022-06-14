package org.mitre.healthmanager.lib.pdr;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

@MessagingGateway
public interface PdrGateway {

	@Gateway(requestChannel = "processMessageChannel", replyTimeout=1000)
	public String processMessage(IBaseBundle theMessage);

}
