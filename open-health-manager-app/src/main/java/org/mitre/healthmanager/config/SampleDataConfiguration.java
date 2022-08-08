package org.mitre.healthmanager.config;

import java.io.IOException;
import java.util.Arrays;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.parser.DataFormatException;

@Component
@Profile("dev)")
public class SampleDataConfiguration implements InitializingBean {

    @Autowired
    private DaoRegistry myDaoRegistry;
    
    @Value("classpath:config/liquibase/fake-data/fhir/**/*.json")
    private Resource[] files;
    
    private final FhirContext fhirContext = FhirContext.forR4();

    @Override
    public void afterPropertiesSet() throws Exception {    	
        Arrays.asList(files)
        	.stream()
        	.forEach(file -> {            		
        		try {
					IBaseResource resource = fhirContext.newJsonParser().parseResource(file.getInputStream());
					IFhirResourceDao<IBaseResource> dao = myDaoRegistry.getResourceDaoOrNull(resource.fhirType());
					dao.update(resource);
				} catch (ConfigurationException | DataFormatException | IOException e) {
					throw new RuntimeException(e);
				}        		
        	});
    }
}