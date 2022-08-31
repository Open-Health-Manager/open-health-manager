package org.mitre.healthmanager.web.rest.errors;

public class FHIROrganizationResourceException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public FHIROrganizationResourceException() {
    	super(ErrorConstants.ORGANIZATION_EXCEPTION, "Unable to save fhir organization resource changes!", "userManagement", "organizationexception");
    }
}
