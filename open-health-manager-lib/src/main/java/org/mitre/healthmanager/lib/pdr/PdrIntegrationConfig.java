package org.mitre.healthmanager.lib.pdr;

import java.util.Collections;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MessageHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.MessageProcessingHeaderValueMessageProcessor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Component
public class PdrIntegrationConfig {
	@Autowired
	AppProperties appProperties;

	@ServiceActivator(inputChannel = "transformChannel")
	public Bundle transformMessage(String message, @Header("messageHeader") MessageHeader messageHeader, @Header String fhirServerBase) {
		// NOTE: this line is the reason the provider doesn't do this itself
		// -- it doesn't know its own address (HapiProperties is JPA server only)
		String serverAddress = appProperties.getServer_address() != null ? appProperties.getServer_address(): fhirServerBase;
		Bundle response = new Bundle();
		response.setType(Bundle.BundleType.MESSAGE);
		MessageHeader newHeader = new MessageHeader();
		newHeader.addDestination().setEndpoint(messageHeader.getSource().getEndpoint());
		newHeader.setSource(new MessageHeader.MessageSourceComponent()
				.setEndpoint(serverAddress + "\\$process-message"));
		newHeader.setResponse( new MessageHeader.MessageHeaderResponseComponent()
				.setCode(MessageHeader.ResponseType.OK));
		response.addEntry().setResource(newHeader);

		return response;
	}

	@ServiceActivator(inputChannel = "rawDataChannel", outputChannel = "transformChannel")
	public Bundle processRawBundle(@Payload Bundle theMessage, @Header("messageHeader") MessageHeader messageHeader) {
		System.out.println("Received message from rawDataChannel");

		return theMessage;
	}

	@Bean
	@Transformer(inputChannel = "messageHeaderChannel", outputChannel = "rawDataChannel")
	public HeaderEnricher getMessageHeader() {
		Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd =
				Collections.singletonMap("messageHeader", new MessageProcessingHeaderValueMessageProcessor(message -> {
					Bundle theMessage = (Bundle) message.getPayload();
					MessageHeader messageHeader = ProcessMessageService.getMessageHeader(theMessage);
			        if (!ProcessMessageService.isPDRMessage(messageHeader)) {
			            throw new UnprocessableEntityException("message event not supported");
			        }
			        return messageHeader;
				}));

		HeaderEnricher enricher = new HeaderEnricher(headersToAdd);
		return enricher;
	}

	@ServiceActivator(inputChannel = "processMessageChannel", outputChannel = "messageHeaderChannel")
	public Bundle validateMessage(@Payload Bundle theMessage) {
		// - payload must be a bundle
		if (!(theMessage instanceof Bundle)) {
			throw new UnprocessableEntityException("bundle not provided to $process-message");
		}

		return theMessage;
	}
}
