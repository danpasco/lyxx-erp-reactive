package io.tahawus.lynx.contacts.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.tahawus.lynx.core.model.AuditableEntity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Dan Pasco
 */

@Entity
@Table(name = "contact")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Person.class, name = "person"),
        @JsonSubTypes.Type(value = Organization.class, name = "organization") }
)
public class Contact extends AuditableEntity {

    // ================================
    // =          Attributes          =
    // ================================
    @Column(length = 150, nullable = false)
    public String name;

    @Column(length = 60)
    public String abbreviation;

    @Column(length = 75)
    public String email;

    @Column(name = "web_site", length = 200)
    public String webSite;

    @OneToMany(mappedBy = "contact", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<Address> addresses = new ArrayList<>();

    public void addAddress(Address address) {
        addresses.add(address);
        address.contact = this;
    }

    public void removeAddress(Address address) {
        addresses.remove(address);
        address.contact = null;
    }

    @OneToMany(mappedBy = "contact", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<Telephone> phones = new ArrayList<>();

    public void addPhone(Telephone phone) {
        phones.add(phone);
        phone.contact = this;
    }

    public void removePhone(Telephone phone) {
        phones.remove(phone);
        phone.contact = null;
    }

    // =============================
    // =          Methods          =
    // =============================

    public static List<Contact> findContainingName(String name) {
        return Contact.list("lower(name) like lower(?1)", "%" + name + "%");
    }

    public static Optional<Contact> findByName(String name) {
        return Contact.find("name", name).firstResultOptional();
    }

    @Override
    public String toString() {
        return "Contact{" +
                "name='" + name + '\'' +
                ", abbreviation=" + abbreviation +
                ", email=" + email +
                ", webSite=" + webSite +
                ", id=" + id +
                ", 'type'=" + (this instanceof Person ? "person " : "organization ") +
                '}';
    }
}
