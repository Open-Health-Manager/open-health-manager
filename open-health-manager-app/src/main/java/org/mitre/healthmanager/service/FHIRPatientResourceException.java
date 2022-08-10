package org.mitre.healthmanager.service;

public class FHIRPatientResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FHIRPatientResourceException(String message) {
        super(message);
    }
}
