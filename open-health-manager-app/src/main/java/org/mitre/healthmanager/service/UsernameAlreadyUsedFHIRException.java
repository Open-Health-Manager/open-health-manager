package org.mitre.healthmanager.service;

public class UsernameAlreadyUsedFHIRException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UsernameAlreadyUsedFHIRException() {
        super("Login identifier already used!");
    }
}
