package org.mitre.healthmanager.service;

public class LoginMatchEmailException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LoginMatchEmailException() {
        super("Login must be the same as email!");
    }
}