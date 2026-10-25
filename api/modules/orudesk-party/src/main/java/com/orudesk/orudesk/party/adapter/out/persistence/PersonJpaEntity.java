package com.orudesk.orudesk.party.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.LocalDate;
import java.util.UUID;

/** One row of party_person. */
@Entity
@Table(name = "party_person")
class PersonJpaEntity {

    @Id
    private UUID id;

    @TenantId   // filled from the logged-in user's token on insert, and added to every query
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 30)
    private String phone;

    @Column(length = 320)
    private String email;

    protected PersonJpaEntity() {
        // JPA needs a no-argument constructor
    }

    PersonJpaEntity(UUID id, String firstName, String lastName, LocalDate dateOfBirth, String phone, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.phone = phone;
        this.email = email;
    }

    UUID getId() { return id; }
    String getFirstName() { return firstName; }
    String getLastName() { return lastName; }
    LocalDate getDateOfBirth() { return dateOfBirth; }
    String getPhone() { return phone; }
    String getEmail() { return email; }
}
