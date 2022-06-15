package org.mitre.healthmanager.lib.config;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.mitre.healthmanager.lib.fhir.OHMJpaRestfulServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.config.r4.BaseR4Config;

@Configuration
@EnableTransactionManagement
public class OHMHapiFhirConfig extends BaseR4Config {	
	@Bean
	OHMJpaRestfulServer jpaRestfulServer() {
		return new OHMJpaRestfulServer();
	}
	
	@Override
	@Bean(name = "mySystemDaoR4")
	public IFhirSystemDao<Bundle, Meta> systemDaoR4() {
		org.mitre.healthmanager.lib.sphr.ProcessMessage retVal = new org.mitre.healthmanager.lib.sphr.ProcessMessage();
		return retVal;
	}
}
