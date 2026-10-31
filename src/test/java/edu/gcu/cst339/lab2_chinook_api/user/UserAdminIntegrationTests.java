package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
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
                .param("passwordChangeRequired", "true"))
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
                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid role")));

        assertThat(userRepository.findById(user.getUserId()).orElseThrow().getRole()).isEqualTo("USER");
    }

    @Test
    void missingUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/admin/users/999999/edit"))
                .andExpect(status().isNotFound());
    }
}
