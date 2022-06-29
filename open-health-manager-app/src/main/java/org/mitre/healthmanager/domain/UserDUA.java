package org.mitre.healthmanager.domain;

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.*;
import javax.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A UserDUA.
 */
@Entity
@Table(name = "user_dua")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class UserDUA implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active;

    @NotNull
    @Column(name = "version", nullable = false)
    private String version;

    @NotNull
    @Column(name = "age_attested", nullable = false)
    private Boolean ageAttested;

    @NotNull
    @Column(name = "active_date", nullable = false)
    private Instant activeDate;

    @Column(name = "revocation_date")
    private Instant revocationDate;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public UserDUA id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getActive() {
        return this.active;
    }

    public UserDUA active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getVersion() {
        return this.version;
    }

    public UserDUA version(String version) {
        this.setVersion(version);
        return this;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getAgeAttested() {
        return this.ageAttested;
    }

    public UserDUA ageAttested(Boolean ageAttested) {
        this.setAgeAttested(ageAttested);
        return this;
    }

    public void setAgeAttested(Boolean ageAttested) {
        this.ageAttested = ageAttested;
    }

    public Instant getActiveDate() {
        return this.activeDate;
    }

    public UserDUA activeDate(Instant activeDate) {
        this.setActiveDate(activeDate);
        return this;
    }

    public void setActiveDate(Instant activeDate) {
        this.activeDate = activeDate;
    }

    public Instant getRevocationDate() {
        return this.revocationDate;
    }

    public UserDUA revocationDate(Instant revocationDate) {
        this.setRevocationDate(revocationDate);
        return this;
    }

    public void setRevocationDate(Instant revocationDate) {
        this.revocationDate = revocationDate;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserDUA user(User user) {
        this.setUser(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserDUA)) {
            return false;
        }
        return id != null && id.equals(((UserDUA) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserDUA{" +
            "id=" + getId() +
            ", active='" + getActive() + "'" +
            ", version='" + getVersion() + "'" +
            ", ageAttested='" + getAgeAttested() + "'" +
            ", activeDate='" + getActiveDate() + "'" +
            ", revocationDate='" + getRevocationDate() + "'" +
            "}";
    }
}
