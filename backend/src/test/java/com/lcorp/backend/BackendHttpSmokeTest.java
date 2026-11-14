package com.lcorp.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Map;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:kr3_smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
    "app.bootstrap.email=smoke-admin@example.org",
    "app.bootstrap.name=Smoke Admin",
    "app.bootstrap.password=temporary-admin-password-2026"
})
@AutoConfigureMockMvc
class BackendHttpSmokeTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void startsContextWithPublicHealthAndProtectedApi() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/audit")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationCanReadButCannotAdminister() throws Exception {
        String email = "smoke-" + UUID.randomUUID() + "@example.org";
        String password = "длинный-пароль-для-теста";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email,
                    "displayName", "Smoke Viewer", "password", password,
                    "roles", List.of("ADMIN")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.roles[0]", is("VIEWER")))
            .andExpect(jsonPath("$.passwordChangeRequired", is(false)));

        String token = login(email, password);
        mvc.perform(get("/api/analytics/summary").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/clients").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Тестовый клиент\",\"phone\":\"70000000000\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void bootstrapAdminCanChangeTemporaryPasswordAndOpenAudit() throws Exception {
        String initialToken = login("smoke-admin@example.org", "temporary-admin-password-2026");
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/change-password").header("Authorization", "Bearer " + initialToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"temporary-admin-password-2026\",\"newPassword\":\"new-admin-password-2026\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + initialToken))
            .andExpect(status().isUnauthorized());

        String newToken = login("smoke-admin@example.org", "new-admin-password-2026");
        mvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Login(email, password))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode response = mapper.readTree(body);
        return response.get("accessToken").asText();
    }

    private record Login(String email, String password) { }
}
