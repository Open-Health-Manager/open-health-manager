package org.mitre.healthmanager.lib.config;

import org.mitre.healthmanager.lib.fhir.OHMJpaRestfulServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;	

@Configuration
public class OHMHapiFhirConfig {
	@Bean
	OHMJpaRestfulServer jpaRestfulServer() {
		return new OHMJpaRestfulServer();
	}
	
	@Bean(name = "mySystemDaoR4")
	public IFhirSystemDao<Bundle, Meta> systemDaoR4() {
		org.mitre.healthmanager.lib.sphr.ProcessMessage retVal = new org.mitre.healthmanager.lib.sphr.ProcessMessage();
		return retVal;
	}
}
