package org.mitre.healthmanager.web.rest.errors;

public class InvalidConsentException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public InvalidConsentException() {
    	super(ErrorConstants.INVALID_CONSENT, "Unable to save consent resource changes!", "userManagement", "invalidconsent");
    }
}