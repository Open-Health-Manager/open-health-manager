package org.mitre.healthmanager.web.rest.errors;

public class LoginMatchEmailException extends BadRequestAlertException {
    private static final long serialVersionUID = 1L;

    public LoginMatchEmailException() {
        super(ErrorConstants.LOGIN_MATCH_EMAIL_ERROR, "Login must be the same as email!", "userManagement", "loginmatchemail");
    }
}
