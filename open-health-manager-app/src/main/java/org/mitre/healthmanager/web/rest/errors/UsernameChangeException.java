package org.mitre.healthmanager.web.rest.errors;

public class UsernameChangeException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public UsernameChangeException() {
        super(ErrorConstants.LOGIN_CHANGED_TYPE, "Login cannot be changed!", "userManagement", "loginChanged");
    }
}
