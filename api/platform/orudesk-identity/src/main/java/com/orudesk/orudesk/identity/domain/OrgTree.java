package com.orudesk.orudesk.identity.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * All org units of one tenant, as a tree. A tenant has tens or hundreds of units, not millions,
 * so the whole tree fits in memory and every question about it is answered here, in plain Java.
 */
public final class OrgTree {

    private final Map<OrgUnitId, OrgUnit> units = new HashMap<>();
    private final Map<OrgUnitId, List<OrgUnit>> children = new HashMap<>();

    public OrgTree(Collection<OrgUnit> all) {
        for (OrgUnit unit : all) {
            units.put(unit.id(), unit);
        }
        for (OrgUnit unit : all) {
            unit.parentId().ifPresent(parent -> children
                    .computeIfAbsent(parent, _ -> new ArrayList<>())
                    .add(unit));
        }
        children.values().forEach(list -> list.sort(Comparator.comparing(OrgUnit::name)));
    }

    public Optional<OrgUnit> root() {
        return units.values().stream().filter(OrgUnit::isRoot).findFirst();
    }

    public Optional<OrgUnit> find(OrgUnitId id) {
        return Optional.ofNullable(units.get(id));
    }

    public OrgUnit get(OrgUnitId id) {
        return find(id).orElseThrow(() -> new OrgUnitNotFoundException(id));
    }

    /** The direct children, sorted by name. */
    public List<OrgUnit> childrenOf(OrgUnitId id) {
        return List.copyOf(children.getOrDefault(id, List.of()));
    }

    public boolean codeTaken(String code) {
        return units.values().stream().anyMatch(unit -> unit.code().equalsIgnoreCase(code.trim()));
    }

    /** The unit itself, its parent, its grandparent... up to the root. */
    public List<OrgUnitId> pathToRoot(OrgUnitId id) {
        Set<OrgUnitId> path = new LinkedHashSet<>();
        OrgUnit current = get(id);
        while (current != null && path.add(current.id())) {   // add() is false on a loop: stop instead of spinning
            current = current.parentId().map(units::get).orElse(null);
        }
        return List.copyOf(path);
    }

    /** True if the unit is the ancestor itself or anywhere below it. */
    public boolean isWithin(OrgUnitId unit, OrgUnitId ancestor) {
        return find(unit).isPresent() && pathToRoot(unit).contains(ancestor);
    }

    /** The unit and everything below it. */
    public Set<OrgUnitId> subtreeOf(OrgUnitId id) {
        Set<OrgUnitId> found = new HashSet<>();
        Deque<OrgUnitId> toVisit = new ArrayDeque<>(List.of(get(id).id()));
        while (!toVisit.isEmpty()) {
            OrgUnitId next = toVisit.pop();
            if (found.add(next)) {
                childrenOf(next).forEach(child -> toVisit.push(child.id()));
            }
        }
        return Set.copyOf(found);
    }
}
