package org.mitre.healthmanager.domain;

import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

/**
 * A FHIRPatientConsent.
 */
@Entity
@Table(name = "fhir_patient_consent")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FHIRPatientConsent implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "fhir_resource", nullable = false)
    private String fhirResource;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne
    private FHIRClient client;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public FHIRPatientConsent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFhirResource() {
        return this.fhirResource;
    }

    public FHIRPatientConsent fhirResource(String fhirResource) {
        this.setFhirResource(fhirResource);
        return this;
    }

    public void setFhirResource(String fhirResource) {
        this.fhirResource = fhirResource;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FHIRPatientConsent user(User user) {
        this.setUser(user);
        return this;
    }

    public FHIRClient getClient() {
        return this.client;
    }

    public void setClient(FHIRClient fHIRClient) {
        this.client = fHIRClient;
    }

    public FHIRPatientConsent client(FHIRClient fHIRClient) {
        this.setClient(fHIRClient);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRPatientConsent)) {
            return false;
        }
        return id != null && id.equals(((FHIRPatientConsent) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRPatientConsent{" +
            "id=" + getId() +
            ", fhirResource='" + getFhirResource() + "'" +
            "}";
    }
}
