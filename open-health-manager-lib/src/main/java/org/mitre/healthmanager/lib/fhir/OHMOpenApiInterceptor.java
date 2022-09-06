package org.mitre.healthmanager.lib.fhir;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

public class OHMOpenApiInterceptor extends OpenApiInterceptor {
	public OHMOpenApiInterceptor() {
		super();
	}
	
	@Override 
	@Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
	public boolean serveSwaggerUi(HttpServletRequest theRequest, HttpServletResponse theResponse, ServletRequestDetails theRequestDetails) throws IOException {
		String requestPath = theRequest.getPathInfo();
		
		if (requestPath.equals("/api-docs")) {

			OpenAPI openApi = generateOpenApi(theRequestDetails);
			openApi.getComponents()
				.addSecuritySchemes("bearerAuth",
						new SecurityScheme()
							.type(SecurityScheme.Type.HTTP)
							.scheme("bearer")
							.bearerFormat("JWT")
						);				
	        openApi.security(List.of(new SecurityRequirement().addList("bearerAuth")));
			String response = Yaml.pretty(openApi);

			theResponse.setContentType("text/yaml");
			theResponse.setStatus(200);
			theResponse.getWriter().write(response);
			theResponse.getWriter().close();
			return false;

		}
		
		return super.serveSwaggerUi(theRequest, theResponse, theRequestDetails);
	}
}
