package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Stores a BCrypt hash, just like a real account, so Spring Security can match "secret" against it.
    private User saveUser(boolean enabled, boolean passwordChangeRequired) {
        User user = new User("login-test-user", passwordEncoder.encode("secret"), "USER");
        user.setEnabled(enabled);
        user.setPasswordChangeRequired(passwordChangeRequired);
        return userRepository.saveAndFlush(user);
    }

    // Also proves the error/logout alerts are conditional: a plain visit must not show them.
    @Test
    void loginPageShowsFormWithoutAlerts() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(not(containsString("Invalid username or password"))))
                .andExpect(content().string(not(containsString("You have been logged out"))));
    }

    @Test
    void loginPageShowsFlashMessage() throws Exception {
        mockMvc.perform(get("/login").flashAttr("successMessage", "Registration successful"))
                .andExpect(content().string(containsString("Registration successful")));
    }

    @Test
    void loginPageShowsErrorAlertWhenErrorParamPresent() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    @Test
    void loginPageShowsLogoutAlertWhenLogoutParamPresent() throws Exception {
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(content().string(containsString("You have been logged out")));
    }

    @Test
    void validLoginRedirectsHomeAndAuthenticates() throws Exception {
        saveUser(true, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret")
                .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("login-test-user").withRoles("USER"));
    }

    @Test
    void wrongPasswordRedirectsToLoginError() throws Exception {
        saveUser(true, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "wrong")
                .with(csrf()))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void unknownUserRedirectsToSameLoginError() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "login-no-such-user")
                .param("password", "secret")
                .with(csrf()))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void disabledUserIsRefused() throws Exception {
        saveUser(false, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret")
                .with(csrf()))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void loginWithoutCsrfTokenIsForbidden() throws Exception {
        saveUser(true, false);

        mockMvc.perform(post("/login")
                .param("username", "login-test-user")
                .param("password", "secret"))
                .andExpect(status().isForbidden());
    }

    // Accept: text/html makes MockMvc look like a browser, so Spring Security
    // uses the form-login entry point (redirect) instead of HTTP Basic (401).
    @Test
    void changePasswordPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/change-password").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // user(...) logs in a mock user for this request, so the controller's Principal is "login-test-user".
    @Test
    void changePasswordUpdatesUserAndRedirectsToLogin() throws Exception {
        saveUser(true, true);

        mockMvc.perform(post("/change-password")
                .with(user("login-test-user"))
                .param("currentPassword", "secret")
                .param("newPassword", "newsecret")
                .param("confirmPassword", "newsecret")
                .with(csrf()))
                .andExpect(redirectedUrl("/login"));

        User user = userRepository.findByUsername("login-test-user").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("newsecret").startsWith("$2");
        assertThat(passwordEncoder.matches("newsecret", user.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("secret", user.getPassword())).isFalse();
        assertThat(user.isPasswordChangeRequired()).isFalse();
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        User saved = saveUser(true, true);
        String originalHash = saved.getPassword();

        mockMvc.perform(post("/change-password")
                .with(user("login-test-user"))
                .param("currentPassword", "wrong")
                .param("newPassword", "newsecret")
                .param("confirmPassword", "newsecret")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Current password is incorrect")));

        User user = userRepository.findByUsername("login-test-user").orElseThrow();
        assertThat(user.getPassword()).isEqualTo(originalHash);
    }
}
