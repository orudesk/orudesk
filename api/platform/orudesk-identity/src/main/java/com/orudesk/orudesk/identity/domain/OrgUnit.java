package com.orudesk.orudesk.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A school, hospital, branch or group inside a tenant. Units form a tree with one root per tenant.
 * Like Person, it has no tenant field: the persistence layer stamps and filters the tenant.
 */
public class OrgUnit {

    // Short, URL-friendly and unique in the tenant: "sunrise-a", "main-campus"
    private static final Pattern CODE = Pattern.compile("^[a-z0-9][a-z0-9-]{1,49}$");
    private static final int MAX_NAME = 200;

    private final OrgUnitId id;
    private final OrgUnitId parentId;
    private final OrgUnitType type;
    private final String code;
    private final String name;

    private OrgUnit(OrgUnitId id, OrgUnitId parentId, OrgUnitType type, String code, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.parentId = parentId;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.code = normaliseCode(code);
        this.name = requireName(name);
    }

    /** A new unit below an existing one. The root is never created here: it comes with the tenant. */
    public static OrgUnit createUnder(OrgUnit parent, OrgUnitType type, String code, String name) {
        Objects.requireNonNull(parent, "parent must not be null");
        return new OrgUnit(OrgUnitId.newId(), parent.id(), type, code, name);
    }

    /** Rebuilds a unit that already exists, for example after loading it from the database. */
    public static OrgUnit restore(OrgUnitId id, OrgUnitId parentId, OrgUnitType type, String code, String name) {
        return new OrgUnit(id, parentId, type, code, name);
    }

    private static String normaliseCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        String normalised = code.trim().toLowerCase(Locale.ROOT);
        if (!CODE.matcher(normalised).matches()) {
            throw new IllegalArgumentException(
                    "invalid org unit code: " + code + " (2 to 50 letters, digits or dashes)");
        }
        return normalised;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > MAX_NAME) {
            throw new IllegalArgumentException("name must be at most " + MAX_NAME + " characters");
        }
        return trimmed;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public OrgUnitId id() { return id; }
    public Optional<OrgUnitId> parentId() { return Optional.ofNullable(parentId); }
    public OrgUnitType type() { return type; }
    public String code() { return code; }
    public String name() { return name; }
}
