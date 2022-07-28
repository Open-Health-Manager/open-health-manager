package org.mitre.healthmanager.service.dto;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Lob;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link org.mitre.healthmanager.domain.FHIRPatientConsent} entity.
 */
public class FHIRPatientConsentDTO implements Serializable {

    private Long id;

    @Lob
    private String fhirResource;

    private UserDTO user;

    private FHIRClientDTO client;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFhirResource() {
        return fhirResource;
    }

    public void setFhirResource(String fhirResource) {
        this.fhirResource = fhirResource;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public FHIRClientDTO getClient() {
        return client;
    }

    public void setClient(FHIRClientDTO client) {
        this.client = client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRPatientConsentDTO)) {
            return false;
        }

        FHIRPatientConsentDTO fHIRPatientConsentDTO = (FHIRPatientConsentDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, fHIRPatientConsentDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRPatientConsentDTO{" +
            "id=" + getId() +
            ", fhirResource='" + getFhirResource() + "'" +
            ", user=" + getUser() +
            ", client=" + getClient() +
            "}";
    }
}
