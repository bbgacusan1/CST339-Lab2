package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
class LoginIntegrationTests {

    private static final String LOGIN_ERROR = "Invalid username or password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User saveUser(boolean enabled, boolean passwordChangeRequired) {
        User user = new User("login-test-user", "secret", "USER");
        user.setEnabled(enabled);
        user.setPasswordChangeRequired(passwordChangeRequired);
        return userRepository.saveAndFlush(user);
    }

    @Test
    void loginPageShowsForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"password\"")));
    }

    @Test
    void loginPageShowsFlashMessage() throws Exception {
        mockMvc.perform(get("/login").flashAttr("successMessage", "Registration successful"))
                .andExpect(content().string(containsString("Registration successful")));
    }

    @Test
    void validLoginRedirectsHome() throws Exception {
        saveUser(true, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void passwordChangeRequiredRedirectsToChangePage() throws Exception {
        saveUser(true, true);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret"))
                .andExpect(redirectedUrl("/change-password"));
    }

    @Test
    void wrongPasswordShowsGeneralError() throws Exception {
        saveUser(true, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "wrong"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LOGIN_ERROR)));
    }

    @Test
    void unknownUserShowsSameGeneralError() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "login-no-such-user")
                .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LOGIN_ERROR)));
    }

    @Test
    void disabledUserIsRefused() throws Exception {
        saveUser(false, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(LOGIN_ERROR)));
    }

    @Test
    void changePasswordPageRedirectsWithoutSession() throws Exception {
        mockMvc.perform(get("/change-password"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void changePasswordUpdatesUserAndRedirectsToLogin() throws Exception {
        saveUser(true, true);

        mockMvc.perform(post("/change-password")
                .sessionAttr("username", "login-test-user")
                .param("currentPassword", "secret")
                .param("newPassword", "newsecret")
                .param("confirmPassword", "newsecret"))
                .andExpect(redirectedUrl("/login"));

        User user = userRepository.findByUsername("login-test-user").orElseThrow();
        assertThat(user.getPassword()).isEqualTo("newsecret");
        assertThat(user.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        saveUser(true, true);

        mockMvc.perform(post("/change-password")
                .sessionAttr("username", "login-test-user")
                .param("currentPassword", "wrong")
                .param("newPassword", "newsecret")
                .param("confirmPassword", "newsecret"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Current password is incorrect")));
    }
}
