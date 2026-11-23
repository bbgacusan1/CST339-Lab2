package edu.gcu.cst339.lab2_chinook_api.album;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

// Branch 7: GET /api/** is allowed for USER and ADMIN; POST/PUT/DELETE are ADMIN only.
// The ADMIN success cases (201/200/204) are covered in AlbumIntegrationTests.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlbumSecurityIntegrationTests {

    private static final String ALBUM_JSON = """
            {"title": "Security Test Album", "artistId": 1}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Album saveAlbum() {
        return albumRepository.saveAndFlush(new Album("Temp Album", 1));
    }

    // The User entity is package-private in the user package, so insert the row with SQL.
    // The password is a real BCrypt hash, just like an account created through /register.
    private void saveUser(String username, String password, String role) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password, role, enabled, password_change_required) VALUES (?, ?, ?, TRUE, FALSE)",
                username, passwordEncoder.encode(password), role);
    }

    // ---- Anonymous ----

    // No Accept: text/html, so this looks like Postman: 401 plus a Basic challenge, not a login redirect.
    @Test
    void anonymousGetIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"));
    }

    // ---- USER: read only ----

    @Test
    @WithMockUser(roles = "USER")
    void userCanGetAllAlbums() throws Exception {
        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanGetAlbumById() throws Exception {
        mockMvc.perform(get("/api/albums/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotCreateAlbum() throws Exception {
        long before = albumRepository.count();

        mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALBUM_JSON))
                .andExpect(status().isForbidden());

        assertThat(albumRepository.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotUpdateAlbum() throws Exception {
        Integer id = saveAlbum().getAlbumId();

        mockMvc.perform(put("/api/albums/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALBUM_JSON))
                .andExpect(status().isForbidden());

        assertThat(albumRepository.findById(id).orElseThrow().getTitle()).isEqualTo("Temp Album");
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotDeleteAlbum() throws Exception {
        Integer id = saveAlbum().getAlbumId();

        mockMvc.perform(delete("/api/albums/" + id))
                .andExpect(status().isForbidden());

        assertThat(albumRepository.existsById(id)).isTrue();
    }

    // ---- Real HTTP Basic (how Postman authenticates) ----

    @Test
    void basicAuthUserCanGetAlbums() throws Exception {
        saveUser("api-test-user", "secret", "USER");

        mockMvc.perform(get("/api/albums").with(httpBasic("api-test-user", "secret")))
                .andExpect(status().isOk());
    }

    @Test
    void basicAuthWrongPasswordIsUnauthorized() throws Exception {
        saveUser("api-test-user", "secret", "USER");

        mockMvc.perform(get("/api/albums").with(httpBasic("api-test-user", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void basicAuthUserCannotCreateAlbum() throws Exception {
        saveUser("api-test-user", "secret", "USER");

        mockMvc.perform(post("/api/albums")
                        .with(httpBasic("api-test-user", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALBUM_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void basicAuthAdminCanCreateAlbum() throws Exception {
        saveUser("api-test-admin", "secret", "ADMIN");

        mockMvc.perform(post("/api/albums")
                        .with(httpBasic("api-test-admin", "secret"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ALBUM_JSON))
                .andExpect(status().isCreated());
    }
}
