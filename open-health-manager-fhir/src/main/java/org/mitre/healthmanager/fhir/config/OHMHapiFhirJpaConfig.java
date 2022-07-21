package org.mitre.healthmanager.fhir.config;

import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;
import ca.uhn.fhir.jpa.starter.FhirTesterConfig;
import ca.uhn.fhir.jpa.starter.annotations.OnEitherVersion;
import ca.uhn.fhir.jpa.starter.mdm.MdmConfig;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.to.util.WebUtil;

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
    protected ApplicationContext applicationContext;
	@Autowired
	AutowireCapableBeanFactory beanFactory;
	@Autowired
	AppProperties appProperties;

	@Bean
	@Conditional(OnEitherVersion.class)
	@ConditionalOnBean(value = BaseJpaRestfulServer.class)
	public ServletRegistrationBean<BaseJpaRestfulServer> hapiServletRegistration(BaseJpaRestfulServer jpaRestfulServer) {
		ServletRegistrationBean<BaseJpaRestfulServer> servletRegistrationBean = new ServletRegistrationBean<BaseJpaRestfulServer>();
		beanFactory.autowireBean(jpaRestfulServer);
		servletRegistrationBean.setServlet(jpaRestfulServer);
		servletRegistrationBean.addUrlMappings("/fhir/*");
		servletRegistrationBean.setLoadOnStartup(1);

		return servletRegistrationBean;
	}

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
