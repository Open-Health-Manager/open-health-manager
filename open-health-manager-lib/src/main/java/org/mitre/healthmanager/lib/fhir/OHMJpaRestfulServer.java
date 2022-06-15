package org.mitre.healthmanager.lib.fhir;

import javax.servlet.ServletException;

import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.BaseJpaRestfulServer;

@Import(AppProperties.class)
public class OHMJpaRestfulServer extends BaseJpaRestfulServer {
	public OHMJpaRestfulServer() {
		super();
	}

	@Override
	protected void initialize() throws ServletException {
		super.initialize();
	}

}
