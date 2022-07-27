package org.mitre.healthmanager.web.rest.errors;

public class InvalidDUAException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public InvalidDUAException() {
    	super(ErrorConstants.INVALID_DUA, "Data use agreement must be active and include age attestation!", "userManagement", "invaliddua");
    }
}