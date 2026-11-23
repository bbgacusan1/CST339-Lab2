package edu.gcu.cst339.lab2_chinook_api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Security behaviors not covered by the access matrix: logout, CSRF, disabled accounts,
 * full CRUD over real HTTP Basic, and password hashing through the real /register endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityBehaviorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // The User entity is package-private in the user package, so insert test accounts with SQL.
    private void saveUser(String username, String password, String role, boolean enabled) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password, role, enabled, password_change_required) VALUES (?, ?, ?, ?, FALSE)",
                username, passwordEncoder.encode(password), role, enabled);
    }

    private String storedPassword(String username) {
        return jdbcTemplate.queryForObject("SELECT password FROM users WHERE username = ?", String.class, username);
    }

    // ---- Logout and CSRF ----

    // Accept: text/html makes this a browser logout (302 to /login?logout).
    // Without it, Spring Security treats the caller as an API client and answers 204 No Content.
    @Test
    void logoutRedirectsToLoginAndEndsSession() throws Exception {
        mockMvc.perform(post("/logout").with(user("behavior-user")).with(csrf())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(unauthenticated());
    }

    @Test
    void logoutWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/logout").with(user("behavior-user")))
                .andExpect(status().isForbidden());
    }

    // CSRF is only ignored for /api/**; browser form POSTs still need the token.
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminFormPostWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/admin/users/1")
                        .param("role", "ADMIN")
                        .param("enabled", "true"))
                .andExpect(status().isForbidden());
    }

    // The REST API is meant for Postman/Swagger, so it must work without a CSRF token.
    @Test
    @WithMockUser(roles = "ADMIN")
    void apiPostWorksWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "No CSRF Album", "artistId": 1}
                                """))
                .andExpect(status().isCreated());
    }

    // ---- HTTP Basic with real accounts ----

    @Test
    void disabledAccountCannotUseHttpBasic() throws Exception {
        saveUser("behavior-disabled", "secret", "USER", false);

        mockMvc.perform(get("/api/albums").with(httpBasic("behavior-disabled", "secret")))
                .andExpect(status().isUnauthorized());
    }

    // Create, update and delete one album with real Basic credentials, the way Postman would.
    @Test
    void basicAuthAdminHasFullCrud() throws Exception {
        saveUser("behavior-admin", "secret", "ADMIN", true);

        String created = mockMvc.perform(post("/api/albums")
                        .with(httpBasic("behavior-admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Basic CRUD Album", "artistId": 1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.albumId");

        mockMvc.perform(put("/api/albums/" + id)
                        .with(httpBasic("behavior-admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Basic CRUD Album Updated", "artistId": 1}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/albums/" + id).with(httpBasic("behavior-admin", "secret")))
                .andExpect(status().isNoContent());
    }

    // ---- Password storage ----

    @Test
    void registrationNeverStoresPlainTextPassword() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "behavior-registered")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(redirectedUrl("/login"));

        String stored = storedPassword("behavior-registered");
        assertThat(stored).isNotEqualTo("secret").startsWith("$2").hasSize(60);
        assertThat(passwordEncoder.matches("secret", stored)).isTrue();
    }
}
