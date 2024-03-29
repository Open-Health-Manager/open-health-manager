package org.mitre.healthmanager.lib.config;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.mitre.healthmanager.lib.fhir.OHMJpaRestfulServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.config.r4.BaseR4Config;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;
import ca.uhn.fhir.jpa.starter.annotations.OnEitherVersion;

@Configuration
@EnableTransactionManagement
public class OHMHapiFhirConfig extends BaseR4Config {	
	@Autowired
	AutowireCapableBeanFactory beanFactory;
	
	@Bean
	@Conditional(OnEitherVersion.class)
	public ServletRegistrationBean<BaseJpaRestfulServer> hapiServletRegistration() {
		BaseJpaRestfulServer jpaRestfulServer = new OHMJpaRestfulServer();
		ServletRegistrationBean<BaseJpaRestfulServer> servletRegistrationBean = new ServletRegistrationBean<BaseJpaRestfulServer>();
		beanFactory.autowireBean(jpaRestfulServer);
		servletRegistrationBean.setServlet(jpaRestfulServer);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		return servletRegistrationBean;
	}
	
	@Override
	@Bean(name = "mySystemDaoR4")
	public IFhirSystemDao<Bundle, Meta> systemDaoR4() {
		org.mitre.healthmanager.lib.sphr.ProcessMessage retVal = new org.mitre.healthmanager.lib.sphr.ProcessMessage();
		return retVal;
	}
}
