package io.tahawus.lynx.contacts.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.Entity;

/**
 * @author Dan Pasco
 * https://tahawus.dev
 */

@Entity
@JsonTypeName("organization")
public class Organization extends Contact {
    // ================================
    // =          Attributes          =
    // ================================

    // =============================
    // =          Methods          =
    // =============================
    @Override
    public String toString() {
        return "Organization{" +
            "name='" + name + '\'' +
            ", abbreviation=" + abbreviation +
            ", email=" + email +
            ", webSite=" + webSite +
            ", id=" + id +
            '}';
    }
}
