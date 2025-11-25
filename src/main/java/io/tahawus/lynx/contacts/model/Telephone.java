package io.tahawus.lynx.contacts.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.tahawus.lynx.core.model.LynxPanacheEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "telephone")
public class Telephone extends LynxPanacheEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "contact_fk")
    @JsonIgnore
    public Contact contact;

    @Column(length = 20)
    public String type;

    @Column(name = "telephone_number", length = 30)
    public String telephoneNumber;

    @Column(length = 100)
    public String description;
}
