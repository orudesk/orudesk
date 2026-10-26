package com.orudesk.orudesk.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgTreeTest {

    //  Sunrise Trust
    //   ├─ Sunrise Group ─┬─ Sunrise School A
    //   │                 └─ Sunrise School B
    //   └─ Sunrise Clinic
    private final OrgUnit trust = OrgUnit.restore(OrgUnitId.newId(), null, new OrgUnitType("ORGANISATION"), "sunrise", "Sunrise Trust");
    private final OrgUnit group = OrgUnit.createUnder(trust, new OrgUnitType("GROUP"), "sunrise-group", "Sunrise Group");
    private final OrgUnit schoolB = OrgUnit.createUnder(group, new OrgUnitType("SCHOOL"), "sunrise-b", "Sunrise School B");
    private final OrgUnit schoolA = OrgUnit.createUnder(group, new OrgUnitType("SCHOOL"), "sunrise-a", "Sunrise School A");
    private final OrgUnit clinic = OrgUnit.createUnder(trust, new OrgUnitType("HOSPITAL"), "clinic", "Sunrise Clinic");

    private final OrgTree tree = new OrgTree(List.of(schoolB, clinic, trust, schoolA, group));

    @Test
    void findsTheRootAndSortsChildrenByName() {
        assertThat(tree.root()).contains(trust);
        assertThat(tree.childrenOf(group.id())).containsExactly(schoolA, schoolB);
    }

    @Test
    void walksFromAUnitUpToTheRoot() {
        assertThat(tree.pathToRoot(schoolA.id())).containsExactly(schoolA.id(), group.id(), trust.id());
    }

    @Test
    void aSchoolIsWithinItsGroupButNotWithinTheClinic() {
        assertThat(tree.isWithin(schoolA.id(), group.id())).isTrue();
        assertThat(tree.isWithin(schoolA.id(), schoolA.id())).isTrue();   // a unit is within itself
        assertThat(tree.isWithin(schoolA.id(), clinic.id())).isFalse();
        assertThat(tree.isWithin(group.id(), schoolA.id())).isFalse();    // a parent is not within its child
    }

    @Test
    void theGroupsSubtreeIsTheGroupAndItsSchools() {
        assertThat(tree.subtreeOf(group.id())).containsExactlyInAnyOrder(group.id(), schoolA.id(), schoolB.id());
    }

    @Test
    void codesAreUniqueIgnoringCase() {
        assertThat(tree.codeTaken("SUNRISE-A")).isTrue();
        assertThat(tree.codeTaken("sunrise-c")).isFalse();
    }

    @Test
    void anUnknownUnitIsNotFound() {
        assertThatThrownBy(() -> tree.get(OrgUnitId.newId())).isInstanceOf(OrgUnitNotFoundException.class);
    }

    @Test
    void refusesABadCode() {
        assertThatThrownBy(() -> OrgUnit.createUnder(trust, new OrgUnitType("SCHOOL"), "Sunrise A!", "Sunrise A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aLoopInTheDataDoesNotHang() {
        OrgUnitId a = OrgUnitId.newId();
        OrgUnitId b = OrgUnitId.newId();
        OrgTree looped = new OrgTree(List.of(
                OrgUnit.restore(a, b, new OrgUnitType("GROUP"), "loop-a", "A"),
                OrgUnit.restore(b, a, new OrgUnitType("GROUP"), "loop-b", "B")));

        assertThat(looped.pathToRoot(a)).containsExactly(a, b);
    }
}
