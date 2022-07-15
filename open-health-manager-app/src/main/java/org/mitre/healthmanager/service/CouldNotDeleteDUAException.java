package org.mitre.healthmanager.service;

public class CouldNotDeleteDUAException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CouldNotDeleteDUAException() {
        super("Could not find or delete DUA for user that is to be deleted.");
    }
}