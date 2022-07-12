package org.mitre.healthmanager.lib.config;

import org.mitre.healthmanager.lib.fhir.OHMJpaRestfulServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.dao.r4.FhirSystemDaoR4;
import ca.uhn.fhir.jpa.config.r4.BaseR4Config;

@Configuration
@EnableTransactionManagement
public class OHMHapiFhirConfig extends BaseR4Config {	
	@Bean
	OHMJpaRestfulServer jpaRestfulServer() {
		return new OHMJpaRestfulServer();
	}
	
	/*
	 * @Override
	 * 
	 * @Bean(name = "mySystemDaoR4") public IFhirSystemDao<Bundle, Meta>
	 * systemDaoR4() { org.mitre.healthmanager.lib.sphr.ProcessMessage retVal = new
	 * org.mitre.healthmanager.lib.sphr.ProcessMessage(); return retVal; }
	 */
}
