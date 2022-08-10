package org.mitre.healthmanager.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link org.mitre.healthmanager.domain.UserDUA} entity.
 */
public class UserDUADTO implements Serializable {

    private Long id;

    @NotNull
    private Boolean active;

    @NotNull
    private String version;

    @NotNull
    private Boolean ageAttested;

    @NotNull
    private Instant activeDate;

    private Instant revocationDate;

    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getAgeAttested() {
        return ageAttested;
    }

    public void setAgeAttested(Boolean ageAttested) {
        this.ageAttested = ageAttested;
    }

    public Instant getActiveDate() {
        return activeDate;
    }

    public void setActiveDate(Instant activeDate) {
        this.activeDate = activeDate;
    }

    public Instant getRevocationDate() {
        return revocationDate;
    }

    public void setRevocationDate(Instant revocationDate) {
        this.revocationDate = revocationDate;
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
        if (!(o instanceof UserDUADTO)) {
            return false;
        }

        UserDUADTO userDUADTO = (UserDUADTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, userDUADTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserDUADTO{" +
            "id=" + getId() +
            ", active='" + getActive() + "'" +
            ", version='" + getVersion() + "'" +
            ", ageAttested='" + getAgeAttested() + "'" +
            ", activeDate='" + getActiveDate() + "'" +
            ", revocationDate='" + getRevocationDate() + "'" +
            ", user=" + getUser() +
            "}";
    }
}
