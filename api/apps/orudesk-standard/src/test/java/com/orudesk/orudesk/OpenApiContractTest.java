package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The OpenAPI contract is what the web app's generated client is built from. This test keeps it tidy, and
 * saves it to build/openapi/orudesk-standard.json so CI can publish it with every build.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void everyOperationHasAStableHandWrittenNameAndTheContractIsSaved() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> operationIds = JsonPath.read(json, "$.paths.*.*.operationId");
        assertThat(operationIds)
                .doesNotHaveDuplicates()
                .allMatch(id -> id.matches("[a-z][a-zA-Z]+"), "camelCase, and no springdoc suffix like register_1")
                .contains("login", "registerPerson", "searchPeople", "getCurrentOrgUnit");

        Map<String, Object> bearer = JsonPath.read(json, "$.components.securitySchemes.bearer");
        assertThat(bearer).containsEntry("scheme", "bearer");
        List<Object> loginSecurity = JsonPath.read(json, "$.paths['/api/v1/auth/login'].post.security");
        assertThat(loginSecurity).isEmpty();   // logging in can't require being logged in

        Path file = Path.of("build/openapi/orudesk-standard.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }

    @Test
    void recordComponentsAreRequiredUnlessNullable() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // A response: every field is always there; the @Nullable ones may be null
        List<String> personRequired = JsonPath.read(json, "$.components.schemas.PersonDetails.required");
        assertThat(personRequired).containsExactlyInAnyOrder("id", "firstName", "lastName", "version");
        List<String> phoneTypes = JsonPath.read(json, "$.components.schemas.PersonDetails.properties.phone.type");
        assertThat(phoneTypes).containsExactlyInAnyOrder("string", "null");

        // A request: the client must send firstName and lastName, and may leave the rest out
        List<String> registerRequired = JsonPath.read(json, "$.components.schemas.RegisterPersonRequest.required");
        assertThat(registerRequired).containsExactlyInAnyOrder("firstName", "lastName");
    }
}
