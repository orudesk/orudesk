package com.orudesk.orudesk.identity.adapter.out.persistence;

import com.orudesk.orudesk.identity.domain.*;
import com.orudesk.orudesk.shared.adapter.out.persistence.CurrentTenantResolver;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import({UserPersistenceAdapter.class, CurrentTenantResolver.class})
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPersistenceAdapterTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsAUserWithItsRoles() {
        User user = User.register(DEMO, new Email("karthikeyan@orudesk.com"), new PasswordHash("hashed"));
        user.assignRole(RoleCode.ADMIN);

        adapter.save(user);
        entityManager.flush();   // send the INSERTs to MariaDB now
        entityManager.clear();   // forget cached objects, so the next read really hits the database

        User loaded = adapter.findById(user.id()).orElseThrow();
        assertThat(loaded.email()).isEqualTo(new Email("karthikeyan@orudesk.com"));
        assertThat(loaded.tenantId()).isEqualTo(DEMO);
        assertThat(loaded.roles()).containsExactlyInAnyOrder(RoleCode.USER, RoleCode.ADMIN);
    }

    @Test
    void knowsWhetherAnEmailIsTaken() {
        adapter.save(User.register(DEMO, new Email("karthikeyan@orudesk.com"), new PasswordHash("hashed")));
        entityManager.flush();

        assertThat(adapter.existsByEmail(new Email("karthikeyan@orudesk.com"))).isTrue();
        assertThat(adapter.existsByEmail(new Email("nobody@orudesk.com"))).isFalse();
    }

    @Test
    void knowsTheSeededRoles() {
        assertThat(adapter.roleExists(RoleCode.ADMIN)).isTrue();
        assertThat(adapter.roleExists(new RoleCode("PILOT"))).isFalse();
    }
}
