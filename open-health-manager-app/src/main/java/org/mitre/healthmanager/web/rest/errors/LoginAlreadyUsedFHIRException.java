package org.mitre.healthmanager.web.rest.errors;

public class LoginAlreadyUsedFHIRException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public LoginAlreadyUsedFHIRException() {
        super(ErrorConstants.LOGIN_ALREADY_USED_FHIR_TYPE, "Login identifier already used!", "userManagement", "userexistsFHIR");
    }
}
