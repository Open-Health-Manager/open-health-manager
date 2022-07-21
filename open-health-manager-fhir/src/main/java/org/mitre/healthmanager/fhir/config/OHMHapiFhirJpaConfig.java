package org.mitre.healthmanager.fhir.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.mdm.MdmConfig;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;

@Configuration
@ComponentScan(basePackages="ca.uhn.fhir.jpa.starter", excludeFilters={
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASPECTJ, pattern = "ca.uhn.fhir.jpa.starter.Application"),
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASPECTJ, pattern = "ca.uhn.fhir.jpa.starter.FhirServerConfigR4"),
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASPECTJ, pattern = "ca.uhn.fhir.jpa.starter.BaseR4Config"),
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASPECTJ, pattern = "ca.uhn.fhir.jpa.starter.FhirTesterConfig"),
		@ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASPECTJ, pattern = "ca.uhn.fhir.to.*")})
@EnableAutoConfiguration(exclude= { ElasticsearchRestClientAutoConfiguration.class })
@Import({SubscriptionSubmitterConfig.class, SubscriptionProcessorConfig.class, SubscriptionChannelConfig.class, MdmConfig.class})
public class OHMHapiFhirJpaConfig {
	@Autowired
	AppProperties appProperties;

	@Bean
	@Primary
	@ConfigurationProperties("application.hapi.datasource") 
	public DataSourceProperties dataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	@Primary
	@ConfigurationProperties("application.hapi.datasource.hikari")
	public DataSource dataSource() {
		return dataSourceProperties().initializeDataSourceBuilder().build();
	}
}
