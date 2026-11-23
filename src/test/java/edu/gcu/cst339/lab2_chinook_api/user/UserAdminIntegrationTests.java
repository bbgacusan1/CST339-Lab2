package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// The admin pages require ADMIN, so most tests run as a mock ADMIN; the access tests at the bottom override it.
// The form POSTs also need a CSRF token, added with .with(csrf()).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "test-admin", roles = "ADMIN")
class UserAdminIntegrationTests {

    private static final String SECRET = "do-not-render-this-value";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User saveUser() {
        return userRepository.saveAndFlush(new User("admin-test-user", SECRET, "USER"));
    }

    @Test
    void listShowsUsersWithoutPasswords() throws Exception {
        saveUser();

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-test-user")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void editPageShowsUserWithoutPassword() throws Exception {
        User user = saveUser();

        mockMvc.perform(get("/admin/users/" + user.getUserId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("admin-test-user")))
                .andExpect(content().string(not(containsString(SECRET))));
    }

    @Test
    void updatePersistsRoleAndEnabledStatus() throws Exception {
        User user = saveUser();

        mockMvc.perform(post("/admin/users/" + user.getUserId())
                .param("role", "ADMIN")
                .param("passwordChangeRequired", "true")
                .with(csrf()))
                .andExpect(redirectedUrl("/admin/users"));

        User updated = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo("ADMIN");
        assertThat(updated.isEnabled()).isFalse();
        assertThat(updated.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void invalidRoleIsRejected() throws Exception {
        User user = saveUser();

        mockMvc.perform(post("/admin/users/" + user.getUserId())
                .param("role", "SUPERUSER")
                .param("enabled", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid role")));

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getRole()).isEqualTo("USER");
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/admin/users/999999/edit"))
                .andExpect(status().isNotFound());
    }

    // Branch 6: /admin/** is ADMIN only. These override the class-level mock ADMIN.

    @Test
    @WithMockUser(username = "plain-user", roles = "USER")
    void userIsForbiddenFromAdminList() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "plain-user", roles = "USER")
    void userCannotUpdateAnotherAccount() throws Exception {
        User user = saveUser();

        mockMvc.perform(post("/admin/users/" + user.getUserId())
                .param("role", "ADMIN")
                .param("enabled", "true")
                .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getRole()).isEqualTo("USER");
    }

    // Accept: text/html makes the request look like a browser, so it gets the login redirect, not a 401.
    @Test
    @WithAnonymousUser
    void anonymousVisitorIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/users").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
