package com.orudesk.orudesk.party.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

    // Optimistic locking (Day 46): Hibernate adds "AND version = ?" to every UPDATE and bumps the number. If no
    // row matched, someone else saved first, and Hibernate throws instead of overwriting their change.
    // A Long, not a long: null means "never saved", so Spring Data's save() knows to INSERT.
    @Version
    private Long version;

    // Computed by PostgreSQL (a generated column): read it in queries, never write it
    @Column(name = "search_text", insertable = false, updatable = false)
    private String searchText;

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

    /** Copies new details onto a row loaded in this transaction; the UPDATE goes out when it commits or flushes. */
    void changeDetails(String firstName, String lastName, LocalDate dateOfBirth, String phone, String email) {
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
    Long getVersion() { return version; }
}
