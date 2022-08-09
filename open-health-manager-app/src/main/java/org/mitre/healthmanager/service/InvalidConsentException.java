package org.mitre.healthmanager.service;

public class InvalidConsentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidConsentException(String message) {
        super(message);
    }
}