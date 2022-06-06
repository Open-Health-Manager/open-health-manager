package org.mitre.healthmanager.domain;

import java.io.Serializable;
import javax.persistence.*;
import javax.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A FHIRPatient.
 */
@Entity
@Table(name = "fhirpatient")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FHIRPatient implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Column(name = "fhir_id")
    private String fhirId;

    @OneToOne(optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public FHIRPatient id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFhirId() {
        return this.fhirId;
    }

    public FHIRPatient fhirId(String fhirId) {
        this.setFhirId(fhirId);
        return this;
    }

    public void setFhirId(String fhirId) {
        this.fhirId = fhirId;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FHIRPatient user(User user) {
        this.setUser(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FHIRPatient)) {
            return false;
        }
        return id != null && id.equals(((FHIRPatient) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FHIRPatient{" +
            "id=" + getId() +
            ", fhirId='" + getFhirId() + "'" +
            "}";
    }
}
