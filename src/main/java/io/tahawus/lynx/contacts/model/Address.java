package io.tahawus.lynx.contacts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.tahawus.lynx.core.model.LynxPanacheEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "address")
public class Address extends LynxPanacheEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contact_fk")
    @JsonIgnore
    public Contact contact;

    @Column(name = "additional_street", length = 60)
    public String additionalStreet;

    @Column(length = 50)
    public String city;

    @Column(length = 50)
    public String country;

    @Column(name = "last_change")
    public Instant lastChange = Instant.now();

    @Column(name = "postal_code", length = 30)
    public String postalCode;

    @Column(length = 50)
    public String state;

    @Column(length = 60)
    public String street;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    public AddressType type;
}
