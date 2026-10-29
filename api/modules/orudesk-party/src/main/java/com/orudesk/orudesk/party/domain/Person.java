package com.orudesk.orudesk.party.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Someone the organisation deals with: a patient, a donor, a student, a guardian. Not a login (that's identity's User).
 * The tenant isn't here on purpose: the persistence layer fills and checks it, so business code can't get it wrong.
 */
public class Person {

    private final PersonId id;
    private final PersonName name;
    private final LocalDate dateOfBirth;
    private final String phone;
    private final String email;
    private final long version;

    private Person(PersonId id, PersonName name, LocalDate dateOfBirth, String phone, String email, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.dateOfBirth = dateOfBirth;
        this.phone = blankToNull(phone);
        this.email = blankToNull(email);
        this.version = version;
    }

    /** A new person. Only the name is required; a date of birth can't be in the future. */
    public static Person register(PersonName name, LocalDate dateOfBirth, String phone, String email, LocalDate today) {
        checkDateOfBirth(dateOfBirth, today);
        return new Person(PersonId.newId(), name, dateOfBirth, phone, email, 0);
    }

    /** Rebuilds a person that already exists, for example after loading it from the database. */
    public static Person restore(PersonId id, PersonName name, LocalDate dateOfBirth, String phone, String email,
                                 long version) {
        return new Person(id, name, dateOfBirth, phone, email, version);
    }

    /**
     * The same person with new details (Day 46): the same rules as registering. A new object rather than setters,
     * so a half-applied change can't exist; the version stays, the database moves it on when it saves.
     */
    public Person changeDetails(PersonName newName, LocalDate newDateOfBirth, String newPhone, String newEmail,
                                LocalDate today) {
        checkDateOfBirth(newDateOfBirth, today);
        return new Person(id, newName, newDateOfBirth, newPhone, newEmail, version);
    }

    private static void checkDateOfBirth(LocalDate dateOfBirth, LocalDate today) {
        if (dateOfBirth != null && dateOfBirth.isAfter(today)) {
            throw new IllegalArgumentException("date of birth must not be in the future");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public PersonId id() { return id; }
    public PersonName name() { return name; }
    public Optional<LocalDate> dateOfBirth() { return Optional.ofNullable(dateOfBirth); }
    public Optional<String> phone() { return Optional.ofNullable(phone); }
    public Optional<String> email() { return Optional.ofNullable(email); }
    /** How many times it has been changed. An edit must name the version it was made on. */
    public long version() { return version; }
}
