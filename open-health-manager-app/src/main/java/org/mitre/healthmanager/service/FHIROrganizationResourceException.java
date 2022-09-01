package org.mitre.healthmanager.service;

public class FHIROrganizationResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FHIROrganizationResourceException(String message) {
        super(message);
    }
}