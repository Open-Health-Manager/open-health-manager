package org.mitre.healthmanager.service;

public class UsernameChangeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UsernameChangeException() {
        super("Login name cannot be changed!");
    }
}
