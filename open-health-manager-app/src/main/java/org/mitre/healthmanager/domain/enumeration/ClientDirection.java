package org.mitre.healthmanager.domain.enumeration;

/**
 * The ClientDirection enumeration.
 */
public enum ClientDirection {
    OUTBOUND("Outbound"),
    INBOUND("Inbound"),
    BIDIRECTIONAL("Bidirectional");

    private final String value;

    ClientDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
