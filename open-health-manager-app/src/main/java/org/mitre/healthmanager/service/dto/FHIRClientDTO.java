package org.mitre.healthmanager.service.dto;

import java.io.Serializable;
import java.util.Objects;

import org.mitre.healthmanager.domain.FHIRClient;
import org.mitre.healthmanager.domain.enumeration.ClientDirection;
import javax.validation.constraints.NotNull;

/**
 * A DTO for the {@link org.mitre.healthmanager.domain.FHIRClient} entity.
 */
public class FHIRClientDTO implements Serializable {

    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String displayName;

    private String uri;
 
    private String fhirOrganizationId;

    private ClientDirection clientDirection;
    
    public FHIRClientDTO() {
    	
    }
    
    public FHIRClientDTO(FHIRClient client) {
        this.id = client.getId();        
        this.name = client.getName();
        this.displayName = client.getDisplayName();
        this.uri = client.getUri();
        this.fhirOrganizationId = client.getFhirOrganizationId();
        this.clientDirection = client.getClientDirection();
    }

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
