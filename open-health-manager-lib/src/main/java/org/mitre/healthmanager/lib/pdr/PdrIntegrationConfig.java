package org.mitre.healthmanager.lib.pdr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.healthmanager.lib.pdr.data.RecordMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.MessageProcessingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.dao.TransactionProcessor;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Component
public class PdrIntegrationConfig {
	private final Logger log = LoggerFactory.getLogger(PdrIntegrationConfig.class);
	
	@Autowired
	AppProperties appProperties;
	
	@Autowired
	DaoRegistry doaRegistry;
	
	@Autowired
	TransactionProcessor myTransactionProcessor;
	
	@Autowired
	PartitionManagementProvider partitionManagementProvider;
	
	@Autowired 
	RecordMatchService recordMatchService;
	
    @Bean
    public IntegrationFlow pdrFlow() {
      return IntegrationFlows.from("processMessageChannel")
    		  .handle(this, "validateMessage")    		  
    		  .transform(getMessageHeaders())
    		  .handle(this, "processRawBundle")
    		  .handle(this, "patientMatch")
    		  .split(this, "splitBundle")
    		  .gateway(sourceEndpointRouterFlow())
    		  .handle(this, "recordMatch")
    		  .aggregate(this)
    		  .handle(this, "writeData")
    		  .handle(this, "successMessage")
              .get();
    }
    
    @Bean
    public IntegrationFlow sourceEndpointRouterFlow() {
        return f -> f.routeToRecipients(r -> r
        		.recipientMessageSelector("healthKitChannel", m ->
                	m.getHeaders().get("sourceEndpoint").equals("urn:apple:health-kit"))
        		.requiresReply(true)
        		.defaultOutputToParentFlow()
        );
    }
	
    @Bean
    public MessageChannel errorChannel() {
      return MessageChannels
    		  .direct()
              .get();
    }
	
    @ServiceActivator(inputChannel = "errorChannel")
    public Bundle handleFailedPdr(Message<MessagingException> message) {
    	log.error(message.getPayload().getMessage(), message.getPayload());
		String serverAddress = appProperties.getServer_address() != null ? appProperties.getServer_address(): (String) message.getHeaders().get("fhirServerBase");
		Bundle response = new Bundle();
		response.setType(Bundle.BundleType.MESSAGE);		
		MessageHeader newHeader = new MessageHeader();
		MessageHeader messageHeader = (MessageHeader) message.getHeaders().get("messageHeader");
		if(messageHeader != null && messageHeader.getSource() != null
				&& messageHeader.getSource().getEndpoint() != null) {
			newHeader.addDestination().setEndpoint(messageHeader.getSource().getEndpoint());	
		}		
		newHeader.setSource(new MessageHeader.MessageSourceComponent()
				.setEndpoint(serverAddress + "\\$process-message"));
		MessageHeader.MessageHeaderResponseComponent headerResponse = new MessageHeader.MessageHeaderResponseComponent()				
				.setCode(MessageHeader.ResponseType.FATALERROR);
		if(messageHeader != null && messageHeader.getIdElement() != null 
				&& messageHeader.getIdElement().getIdPart() != null) {
			headerResponse.setIdentifier(messageHeader.getIdElement().getIdPart());
		}
		newHeader.setResponse(headerResponse);
		response.addEntry().setResource(newHeader);

		return response;
    }
	
	@ServiceActivator
	public Bundle successMessage(@Payload Bundle theMessage, @Header("messageHeader") MessageHeader messageHeader, @Header String fhirServerBase) {
		// NOTE: this line is the reason the provider doesn't do this itself
		// -- it doesn't know its own address (HapiProperties is JPA server only)
		String serverAddress = appProperties.getServer_address() != null ? appProperties.getServer_address(): fhirServerBase;
		Bundle response = new Bundle();
		response.setType(Bundle.BundleType.MESSAGE);
		MessageHeader newHeader = new MessageHeader();
		newHeader.addDestination().setEndpoint(messageHeader.getSource().getEndpoint());
		newHeader.setSource(new MessageHeader.MessageSourceComponent()
				.setEndpoint(serverAddress + "\\$process-message"));
		MessageHeader.MessageHeaderResponseComponent headerResponse = new MessageHeader.MessageHeaderResponseComponent()				
				.setCode(MessageHeader.ResponseType.OK);
		if(messageHeader.getIdElement() != null && messageHeader.getIdElement().getIdPart() != null) {
			headerResponse.setIdentifier(messageHeader.getIdElement().getIdPart());
		}
		newHeader.setResponse(headerResponse);
		response.addEntry().setResource(newHeader);

		return response;
	}
	
	@ServiceActivator
	@Transactional
	public Bundle writeData(@Payload Bundle theMessage, @Header("messageHeader") MessageHeader messageHeader,
			@Header("internalPatientId") @NotNull String internalPatientId,
			@Header("rawBundleId") @NotNull String rawBundleId) {	    	    	   
	    Bundle transactionResponse = PatientDataReceiptService.storeIndividualPDREntries(theMessage, internalPatientId, myTransactionProcessor, messageHeader, doaRegistry);
	    PatientDataReceiptService.createPDRList(theMessage, transactionResponse, internalPatientId, rawBundleId, messageHeader, doaRegistry);
		return theMessage;		
	}
	
	@Aggregator
	public Bundle aggregatingMethod(List<BundleEntryComponent> items) {
	    Bundle result = new Bundle();
		for(BundleEntryComponent item: items) {
			result.addEntry(item);
	    }
		return result;
	}
	
	@ServiceActivator
	public BundleEntryComponent recordMatch(@Payload BundleEntryComponent entry, @Header("messageHeader") MessageHeader messageHeader,
			@Header("internalPatientId") @NotNull String internalPatientId) {
		return recordMatchService.recordMatch(entry, internalPatientId, messageHeader, doaRegistry);		
	}
		
	@ServiceActivator
	public HeaderValueRouter sourceEndpointRouter() {
	    HeaderValueRouter router = new HeaderValueRouter("sourceEndpoint");
	    router.setResolutionRequired(false);	    
	    router.setChannelMapping("urn:apple:health-kit", "healthKitChannel");
	    return router;
	}
	  
	@Splitter
	public List<BundleEntryComponent> splitBundle(@Payload Bundle theMessage) {
		// skip message header entry
	    return theMessage.getEntry().stream().skip(1).collect(Collectors.toList());
	}
	
	@SuppressWarnings("unchecked")
	@ServiceActivator
	public Bundle patientMatch(@Payload Bundle theMessage, @Header("messageHeader") MessageHeader messageHeader, @Header("internalPatientId") String internalPatientId) {
		try {
			PatientDataReceiptService.getPatient(internalPatientId, doaRegistry.getDaoOrThrowException(Patient.class));
			return theMessage;
		} catch (ResourceNotFoundException e) {
			throw new UnprocessableEntityException("patient " + internalPatientId + " does not exist");
		}		
	}

	@ServiceActivator
	public Message<Bundle> processRawBundle(Message<Bundle> message, @Headers Map<String,Object> headers) {
		PatientDataReceiptService.validatePDR(message.getPayload());
		@SuppressWarnings("unchecked")
		IFhirResourceDao<Bundle> bundleDao = doaRegistry.getDaoOrThrowException(Bundle.class);
		String rawBundleId = PatientDataReceiptService.storePDRAsRawBundle(message.getPayload(), 
				(@NotNull String) headers.get("internalPatientId"), 
				bundleDao);
		return MutableMessageBuilder.fromMessage(message).copyHeaders(headers).setHeader("rawBundleId", rawBundleId).build();
	}

	@Bean
	public HeaderEnricher getMessageHeaders() {
		Map<String, HeaderValueMessageProcessor<?>> headersToAdd = new HashMap<>();
		headersToAdd.put("messageHeader", 
			new MessageProcessingHeaderValueMessageProcessor(message -> {
				Bundle theMessage = (Bundle) message.getPayload();
				MessageHeader messageHeader = ProcessMessageService.getMessageHeader(theMessage);
		        if (!PatientDataReceiptService.isPDRMessage(messageHeader)) {
		            throw new UnprocessableEntityException("message event not supported");
		        }
		        return messageHeader;
			}));

		headersToAdd.put("internalPatientId", 
			new MessageProcessingHeaderValueMessageProcessor(message -> {
				Bundle theMessage = (Bundle) message.getPayload();
				MessageHeader messageHeader = ProcessMessageService.getMessageHeader(theMessage);
				if (messageHeader.getFocusFirstRep() == null) {
					throw new UnprocessableEntityException("missing patient id");
			    }
				return messageHeader.getFocusFirstRep().getReferenceElement().getIdPart();
			})
		);
		
		headersToAdd.put("sourceEndpoint", 
			new MessageProcessingHeaderValueMessageProcessor(message -> {
				Bundle theMessage = (Bundle) message.getPayload();
				MessageHeader messageHeader = ProcessMessageService.getMessageHeader(theMessage);				
				if (messageHeader.getSource() == null || messageHeader.getSource().getEndpoint() == null) {
					throw new UnprocessableEntityException("missing source endpoint");
				}
				return messageHeader.getSource().getEndpoint();
			})
		);
				
		HeaderEnricher enricher = new HeaderEnricher(headersToAdd);
		return enricher;
	}

	@ServiceActivator
	public Bundle validateMessage(@Payload IBaseBundle theMessage) {
		// - payload must be a bundle
		if (!(theMessage instanceof Bundle)) {
			throw new UnprocessableEntityException("bundle not provided to $process-message");
		}

		return (Bundle) theMessage;
	}
}
