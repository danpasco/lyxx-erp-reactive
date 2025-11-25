package io.tahawus.lynx.contacts.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.persistence.Entity;

/**
 * @author Dan Pasco
 * <a href="https://tahawus.dev">...</a>
 */

@Entity
@JsonTypeName("person")
public class Person extends Contact {
    // ================================
    // =          Attributes          =
    // ================================

    // =============================
    // =          Methods          =
    // =============================
    @Override
    public String toString() {
        return "Person{" +
            "name='" + name + '\'' +
            ", abbreviation=" + abbreviation +
            ", email=" + email +
            ", webSite=" + webSite +
            ", id=" + id +
            '}';
    }
}
