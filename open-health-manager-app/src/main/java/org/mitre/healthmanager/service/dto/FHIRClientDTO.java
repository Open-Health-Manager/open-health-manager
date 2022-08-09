package org.mitre.healthmanager.service.dto;

import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.*;
import org.mitre.healthmanager.domain.enumeration.ClientDirection;

/**
 * A DTO for the {@link org.mitre.healthmanager.domain.FHIRClient} entity.
 */
public class FHIRClientDTO implements Serializable {

    private Long id;

    private String name;

    private String displayName;

    private String uri;

    private String fhirOrganizationId;

    private ClientDirection clientDirection;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getFhirOrganizationId() {
        return fhirOrganizationId;
    }

    public void setFhirOrganizationId(String fhirOrganizationId) {
        this.fhirOrganizationId = fhirOrganizationId;
    }

    public ClientDirection getClientDirection() {
        return clientDirection;
    }

    public void setClientDirection(ClientDirection clientDirection) {
        this.clientDirection = clientDirection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRClientDTO)) {
            return false;
        }

        FHIRClientDTO fHIRClientDTO = (FHIRClientDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, fHIRClientDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRClientDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", uri='" + getUri() + "'" +
            ", fhirOrganizationId='" + getFhirOrganizationId() + "'" +
            ", clientDirection='" + getClientDirection() + "'" +
            "}";
    }
}
