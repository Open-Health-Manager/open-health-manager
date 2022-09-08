package org.mitre.healthmanager.lib.pdr;

import org.assertj.core.api.Assertions;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.MessageHeader;
import org.mitre.healthmanager.lib.TestAuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.client.api.IGenericClient;

@ContextConfiguration(classes={org.mitre.healthmanager.lib.pdr.BasePdrIT.TestAuthConfig.class})
public abstract class BasePdrIT {
	@Autowired
	protected FhirContext ourCtx;
	
	protected IGenericClient testClient;
	
	protected void initClient(int port) {
		if(testClient == null) {
			ourCtx.getRestfulClientFactory().setSocketTimeout(200 * 1000);			
			testClient = ourCtx.newRestfulGenericClient("http://localhost:" + port + "/fhir/");
		}
	}
	
	protected Bundle processMessage(Bundle testMessage) {		
		return testClient
                .operation()
                .processMessage()
                .setMessageBundle(testMessage)
                .synchronous(Bundle.class)
                .execute();
	}
	
	protected void assertSuccessResponse(IBaseBundle result) {
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).isInstanceOf(Bundle.class);
		Assertions.assertThat(((Bundle)result).getEntryFirstRep()).isNotNull();
		Assertions.assertThat(((Bundle)result).getEntryFirstRep().getResource()).isInstanceOf(MessageHeader.class);
		MessageHeader messageHeader = (MessageHeader) ((Bundle)result).getEntryFirstRep().getResource();
		Assertions.assertThat(messageHeader.getResponse().getCode()).isEqualTo(MessageHeader.ResponseType.OK);
	}
	
	protected void assertFailureResponse(IBaseBundle result) {
		Assertions.assertThat(result).isNotNull();
		Assertions.assertThat(result).isInstanceOf(Bundle.class);
		Assertions.assertThat(((Bundle)result).getEntryFirstRep()).isNotNull();
		Assertions.assertThat(((Bundle)result).getEntryFirstRep().getResource()).isInstanceOf(MessageHeader.class);
		MessageHeader messageHeader = (MessageHeader) ((Bundle)result).getEntryFirstRep().getResource();
		Assertions.assertThat(messageHeader.getResponse().getCode()).isEqualTo(MessageHeader.ResponseType.FATALERROR);
	}
	
	protected void assertExistsList(IBaseBundle request, IBaseBundle response) {
		MessageHeader messageHeader = ProcessMessageService.getMessageHeader((Bundle) request);		
		String patientInternalId = messageHeader.getFocusFirstRep().getReferenceElement().getIdPart();
		Identifier identifier = PatientDataReceiptService.getSourceMessageHeaderIdentifier(messageHeader);
		String searchUrl = String.format("List?subject=Patient/%s&code=%s|%s", 
				patientInternalId, PatientDataReceiptService.PDR_CODE.getSystem(), PatientDataReceiptService.PDR_CODE.getCode());		
		if(identifier != null) {
			searchUrl += String.format("&identifier=%s|%s", 
					identifier.getSystem(), identifier.getValue());
		}
		Bundle bundle = testClient.search()
			.byUrl(searchUrl)
			//assumes sequential commits, gets latest
			.sort(new SortSpec().setOrder(SortOrderEnum.DESC).setParamName("date"))
			.returnBundle(Bundle.class).execute();	
				
		Assertions.assertThat(bundle.getEntryFirstRep()).isNotNull();		
		Assertions.assertThat(bundle.getEntryFirstRep().getResource()).isInstanceOf(ListResource.class);
		ListResource pdrList = (ListResource) bundle.getEntryFirstRep().getResource();				
		if(identifier != null) {
			Assertions.assertThat(pdrList.getIdentifierFirstRep().getSystem()).isEqualTo(identifier.getSystem());
			Assertions.assertThat(pdrList.getIdentifierFirstRep().getValue()).isEqualTo(identifier.getValue());
		}
		Assertions.assertThat(pdrList.getStatus()).isEqualTo(ListResource.ListStatus.CURRENT);
		Assertions.assertThat(pdrList.getMode()).isEqualTo(ListResource.ListMode.SNAPSHOT);
		Assertions.assertThat(pdrList.getCode().getCodingFirstRep().getSystem()).isEqualTo(PatientDataReceiptService.PDR_CODE.getSystem());
		Assertions.assertThat(pdrList.getCode().getCodingFirstRep().getCode()).isEqualTo(PatientDataReceiptService.PDR_CODE.getCode());
		Assertions.assertThat(pdrList.getSubject().getReferenceElement().getIdPart()).isEqualTo(patientInternalId);
		Assertions.assertThat(pdrList.getDateElement().getValueAsString()).isNotEmpty();
		long listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> item.getReferenceElement().getResourceType().equals("Bundle"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(1);
		listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> !item.getReferenceElement().getResourceType().equals("Provenance"))
				.filter(item -> !item.getReferenceElement().getResourceType().equals("Bundle"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(((Bundle) request).getEntry().size() - 1);
		listSize = pdrList.getEntry().stream()
				.map(entry -> entry.getItem())
				.filter(item -> item.getReferenceElement().getResourceType().equals("Provenance"))
				.count();
		Assertions.assertThat(listSize).isEqualTo(((Bundle) request).getEntry().size() - 1);
	}
	
    @TestConfiguration(proxyBeanMethods = false)
    protected static class TestAuthConfig {
		public static TestAuthorizationFilter testAuthAdminFilter;
		
        @Bean
        public FilterRegistrationBean<TestAuthorizationFilter> authAdminFilter()
        {
            FilterRegistrationBean<TestAuthorizationFilter> filterBean 
            	= new FilterRegistrationBean<>();
            testAuthAdminFilter = new TestAuthorizationFilter();
            filterBean.setFilter(testAuthAdminFilter);
            filterBean.addUrlPatterns("/*");
            filterBean.setOrder(1);
            return filterBean;    
        }
    }
}
