package org.mitre.healthmanager.domain;

import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.mitre.healthmanager.domain.enumeration.ClientDirection;

/**
 * A FHIRClient.
 */
@Entity
@Table(name = "fhir_client")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FHIRClient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "uri")
    private String uri;

    @NotNull
    @Column(name = "fhir_organization_id", nullable = false)
    private String fhirOrganizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_direction")
    private ClientDirection clientDirection;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public FHIRClient id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public FHIRClient name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public FHIRClient displayName(String displayName) {
        this.setDisplayName(displayName);
        return this;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUri() {
        return this.uri;
    }

    public FHIRClient uri(String uri) {
        this.setUri(uri);
        return this;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getFhirOrganizationId() {
        return this.fhirOrganizationId;
    }

    public FHIRClient fhirOrganizationId(String fhirOrganizationId) {
        this.setFhirOrganizationId(fhirOrganizationId);
        return this;
    }

    public void setFhirOrganizationId(String fhirOrganizationId) {
        this.fhirOrganizationId = fhirOrganizationId;
    }

    public ClientDirection getClientDirection() {
        return this.clientDirection;
    }

    public FHIRClient clientDirection(ClientDirection clientDirection) {
        this.setClientDirection(clientDirection);
        return this;
    }

    public void setClientDirection(ClientDirection clientDirection) {
        this.clientDirection = clientDirection;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRClient)) {
            return false;
        }
        return id != null && id.equals(((FHIRClient) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRClient{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", uri='" + getUri() + "'" +
            ", fhirOrganizationId='" + getFhirOrganizationId() + "'" +
            ", clientDirection='" + getClientDirection() + "'" +
            "}";
    }
}
