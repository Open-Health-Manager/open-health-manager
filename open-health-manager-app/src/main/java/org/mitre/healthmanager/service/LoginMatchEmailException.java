package org.mitre.healthmanager.service;

import javax.validation.ConstraintDeclarationException;

public class LoginMatchEmailException extends ConstraintDeclarationException {
    private static final long serialVersionUID = 1L;

    public LoginMatchEmailException() {
        super("Login must be the same as email!");
    }
}