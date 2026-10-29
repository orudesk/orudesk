package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserCommand;
import com.orudesk.orudesk.identity.application.port.in.RegisterUserUseCase;
import com.orudesk.orudesk.identity.domain.Base32;
import com.orudesk.orudesk.identity.domain.Email;
import com.orudesk.orudesk.identity.domain.TenantId;
import com.orudesk.orudesk.identity.domain.Totp;
import com.orudesk.orudesk.identity.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Days 67-68: two-factor sign-in with an authenticator app, real PostgreSQL and Redis. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TwoFactorIntegrationTest {

    private static final TenantId DEMO = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RegisterUserUseCase registerUser;

    private UserId asha;
    private String token;

    @BeforeEach
    void user() throws Exception {
        asha = registerUser.register(new RegisterUserCommand(DEMO, new Email("test-2fa-asha@orudesk.org"), "asha-password"));
        String body = login()
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.accessToken");
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-2fa-%@orudesk.org'");
    }

    @Test
    void setUpConfirmWithTheAppsCodeThenTurnOff() throws Exception {
        String secret = setup();

        twoFactor().andExpect(jsonPath("$.available").value(true)).andExpect(jsonPath("$.enabled").value(false));

        change("/confirm", "{\"code\": \"000000\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists());

        String codes = change("/confirm", "{\"code\": \"" + codeNow(secret) + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoveryCodes", hasSize(10)))
                .andReturn().getResponse().getContentAsString();
        List<String> recoveryCodes = JsonPath.read(codes, "$.recoveryCodes");

        twoFactor().andExpect(jsonPath("$.enabled").value(true)).andExpect(jsonPath("$.recoveryCodesLeft").value(10));
        String stored = jdbc.queryForObject("SELECT secret_sealed FROM iam_user_totp WHERE user_id = ?", String.class,
                asha.value());
        assertThat(stored).startsWith("v1:").doesNotContain(secret);                 // sealed, not plain
        Integer plainCodes = jdbc.queryForObject("SELECT COUNT(*) FROM iam_recovery_code WHERE code_hash = ?",
                Integer.class, recoveryCodes.getFirst());
        assertThat(plainCodes).isZero();                                             // hashed, not plain

        change("/setup", "{\"password\": \"asha-password\"}")
                .andExpect(status().isConflict());

        change("/disable", "{\"password\": \"asha-password\"}").andExpect(status().isNoContent());
        twoFactor().andExpect(jsonPath("$.enabled").value(false)).andExpect(jsonPath("$.recoveryCodesLeft").value(0));
        Integer audited = jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE subject_id = ?"
                + " AND action IN ('identity.two-factor.enabled', 'identity.two-factor.disabled')", Integer.class, asha.value());
        assertThat(audited).isEqualTo(2);
    }

    @Test
    void withTwoFactorOnTheLoginAsksForACodeAndARecoveryCodeWorksOnce() throws Exception {
        String secret = setup();
        String codes = change("/confirm", "{\"code\": \"" + codeNow(secret) + "\"}")
                .andReturn().getResponse().getContentAsString();
        String recoveryCode = JsonPath.<List<String>>read(codes, "$.recoveryCodes").getFirst();

        String first = login().andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Second factor required"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String loginToken = JsonPath.read(first, "$.loginToken");

        secondStep(loginToken, "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").exists());
        secondStep(loginToken, recoveryCode)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        secondStep(loginToken, recoveryCode)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Login expired"));

        twoFactor().andExpect(jsonPath("$.recoveryCodesLeft").value(9));
        Integer audited = jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE subject_id = ?"
                + " AND action = 'identity.two-factor.recovery-code-used'", Integer.class, asha.value());
        assertThat(audited).isEqualTo(1);
    }

    @Test
    void theWrongPasswordChangesNothing() throws Exception {
        change("/setup", "{\"password\": \"not-it\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM iam_user_totp WHERE user_id = ?", Integer.class, asha.value()))
                .isZero();
    }

    private String setup() throws Exception {
        String body = change("/setup", "{\"password\": \"asha-password\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpauthUri").value(startsWith("otpauth://totp/Orudesk")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.secret");
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"test-2fa-asha@orudesk.org\", \"password\": \"asha-password\"}"));
    }

    private ResultActions secondStep(String loginToken, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login/two-factor").contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginToken\": \"" + loginToken + "\", \"code\": \"" + code + "\"}"));
    }

    private static String codeNow(String secret) {
        return Totp.code(Base32.decode(secret), Totp.step(Instant.now()));
    }

    private ResultActions twoFactor() throws Exception {
        return mockMvc.perform(get("/api/v1/users/me/two-factor").header("Authorization", "Bearer " + token));
    }

    private ResultActions change(String path, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/users/me/two-factor" + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}
