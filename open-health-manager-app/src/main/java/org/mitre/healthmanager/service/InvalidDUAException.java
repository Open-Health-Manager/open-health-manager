package org.mitre.healthmanager.service;

public class InvalidDUAException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDUAException() {
        super("An inactive DUA or a DUA without an attested age cannot be given to register new user.");
    }
}