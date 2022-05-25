package org.mitre.healthmanager.fhir.config;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import ca.uhn.fhir.jpa.starter.FhirServerConfigR4;

@Configuration
public class OHMFhirServerConfigR4 extends FhirServerConfigR4 {
	  @Override
	  @Bean()
	  @Primary
	  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
		  ConfigurableListableBeanFactory myConfigurableListableBeanFactory) {
		  return super.entityManagerFactory(myConfigurableListableBeanFactory);
	  }

}
