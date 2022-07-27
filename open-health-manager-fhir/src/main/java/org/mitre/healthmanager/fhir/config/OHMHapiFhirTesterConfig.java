package org.mitre.healthmanager.fhir.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.FhirTesterConfig;
import ca.uhn.fhir.to.util.WebUtil;

@Configuration
@ConditionalOnExpression("'${hapi.fhir.tester}' != null")
public class OHMHapiFhirTesterConfig {
	@Autowired
	AppProperties appProperties;
	
	@Bean 
	public ServletRegistrationBean<DispatcherServlet> overlayRegistrationBean() {
		AnnotationConfigWebApplicationContext annotationConfigServletWebApplicationContext =
				new AnnotationConfigWebApplicationContext();
		annotationConfigServletWebApplicationContext.register(FhirTesterConfig.class);
		annotationConfigServletWebApplicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor() {
		    @Override
		    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		        beanFactory.registerSingleton("appProperties", appProperties);
		        beanFactory.registerSingleton("testerTemplateResolver", testerTemplateResolver());
		    }});

		DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigServletWebApplicationContext);
		ServletRegistrationBean<DispatcherServlet> registrationBean = new ServletRegistrationBean<DispatcherServlet>();
		registrationBean.setName("overlayDispatcher");
		registrationBean.setServlet(dispatcherServlet);
		registrationBean.addUrlMappings("/tester/*");
		registrationBean.setLoadOnStartup(1); 
		return registrationBean; 
	}
	
	@Bean
	public SpringResourceTemplateResolver testerTemplateResolver() {
		SpringResourceTemplateResolver secondaryTemplateResolver = new SpringResourceTemplateResolver();
	    secondaryTemplateResolver.setPrefix("classpath:/WEB-INF/templates/");
	    secondaryTemplateResolver.setSuffix(".html");
	    secondaryTemplateResolver.setTemplateMode(TemplateMode.HTML);
	    secondaryTemplateResolver.setCharacterEncoding("UTF-8");
	    secondaryTemplateResolver.setOrder(1);
	    secondaryTemplateResolver.setCheckExistence(true);
	        
	    return secondaryTemplateResolver;
	}
	
	// map tester files to root resources path (hardcoded in templates)
	@Configuration
	static class TesterStaticResourcesWebConfiguration implements WebMvcConfigurer {
	    @Override
	    public void addResourceHandlers(ResourceHandlerRegistry registry) {
			WebUtil.webJarAddBoostrap(registry);
			WebUtil.webJarAddJQuery(registry);
			WebUtil.webJarAddFontAwesome(registry);
			WebUtil.webJarAddJSTZ(registry);
			WebUtil.webJarAddEonasdanBootstrapDatetimepicker(registry);
			WebUtil.webJarAddMomentJS(registry);
			WebUtil.webJarAddSelect2(registry);
			WebUtil.webJarAddAwesomeCheckbox(registry);
			WebUtil.webJarAddPopperJs(registry);
			
			registry.addResourceHandler("/tester/css/**").addResourceLocations("classpath:/css/");
			registry.addResourceHandler("/tester/fa/**").addResourceLocations("classpath:/fa/");
			registry.addResourceHandler("/tester/fonts/**").addResourceLocations("classpath:/fonts/");
			registry.addResourceHandler("/tester/img/**").addResourceLocations("classpath:/img/");
			registry.addResourceHandler("/tester/js/**").addResourceLocations("classpath:/js/");

	    }
	}	
}
