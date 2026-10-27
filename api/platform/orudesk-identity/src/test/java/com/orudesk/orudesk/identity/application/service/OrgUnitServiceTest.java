package com.orudesk.orudesk.identity.application.service;

import com.orudesk.orudesk.identity.application.port.in.OrgUnitCreated;
import com.orudesk.orudesk.identity.application.port.in.CreateOrgUnitCommand;
import com.orudesk.orudesk.identity.application.port.in.OrgUnitNode;
import com.orudesk.orudesk.identity.domain.OrgUnit;
import com.orudesk.orudesk.identity.domain.OrgUnitCodeTakenException;
import com.orudesk.orudesk.identity.domain.OrgUnitId;
import com.orudesk.orudesk.identity.domain.OrgUnitNotFoundException;
import com.orudesk.orudesk.identity.domain.OrgUnitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrgUnitServiceTest {

    private final InMemoryOrgUnitStore store = new InMemoryOrgUnitStore();
    private final List<Object> events = new ArrayList<>();
    private final OrgUnitService service = new OrgUnitService(store, store, events::add);
    private final OrgUnit root = OrgUnit.restore(OrgUnitId.newId(), null, new OrgUnitType("ORGANISATION"), "sunrise", "Sunrise Trust");

    @BeforeEach
    void aTenantWithARoot() {
        store.save(root);
    }

    @Test
    void createsSchoolsUnderAGroupAndReturnsTheTree() {
        UUID group = service.create(new CreateOrgUnitCommand(root.id().value(), "group", "sunrise-group", "Sunrise Group"));
        service.create(new CreateOrgUnitCommand(group, "SCHOOL", "sunrise-a", "Sunrise School A"));

        OrgUnitNode tree = service.getTree();

        assertThat(tree.name()).isEqualTo("Sunrise Trust");
        OrgUnitNode groupNode = tree.children().getFirst();
        assertThat(groupNode.type()).isEqualTo("GROUP");   // normalised to upper case
        assertThat(groupNode.children()).extracting(OrgUnitNode::code).containsExactly("sunrise-a");
        assertThat(events).extracting(event -> ((OrgUnitCreated) event).summary()).containsExactly(
                "Created group Sunrise Group under Sunrise Trust",
                "Created school Sunrise School A under Sunrise Group");
    }

    @Test
    void refusesAParentItCannotSee() {
        assertThatThrownBy(() -> service.create(new CreateOrgUnitCommand(UUID.randomUUID(), "SCHOOL", "x-school", "X")))
                .isInstanceOf(OrgUnitNotFoundException.class);
    }

    @Test
    void refusesADuplicateCode() {
        service.create(new CreateOrgUnitCommand(root.id().value(), "SCHOOL", "sunrise-a", "Sunrise School A"));

        assertThatThrownBy(() -> service.create(new CreateOrgUnitCommand(root.id().value(), "SCHOOL", "Sunrise-A", "Another A")))
                .isInstanceOf(OrgUnitCodeTakenException.class);
    }
}
