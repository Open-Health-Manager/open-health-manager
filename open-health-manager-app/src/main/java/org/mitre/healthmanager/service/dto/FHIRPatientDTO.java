package org.mitre.healthmanager.service.dto;

import java.io.Serializable;
import java.util.Objects;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link org.mitre.healthmanager.domain.FHIRPatient} entity.
 */
public class FHIRPatientDTO implements Serializable {

    private Long id;

    @NotNull
    private String fhirId;

    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFhirId() {
        return fhirId;
    }

    public void setFhirId(String fhirId) {
        this.fhirId = fhirId;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRPatientDTO)) {
            return false;
        }

        FHIRPatientDTO fHIRPatientDTO = (FHIRPatientDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, fHIRPatientDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRPatientDTO{" +
            "id=" + getId() +
            ", fhirId='" + getFhirId() + "'" +
            ", user=" + getUser() +
            "}";
    }
}
