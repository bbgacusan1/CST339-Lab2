package edu.gcu.cst339.lab2_chinook_api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationPageShowsForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("confirmPassword")));
    }

    @Test
    void validRegistrationSavesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "reg-test-user")
                .param("password", "secret")
                .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findByUsername("reg-test-user")).isPresent();
    }

    @Test
    void tamperedRoleIsIgnored() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "reg-test-user")
                .param("password", "secret")
                .param("confirmPassword", "secret")
                .param("role", "ADMIN"));

        assertThat(userRepository.findByUsername("reg-test-user").orElseThrow().getRole()).isEqualTo("USER");
    }

    @Test
    void mismatchedPasswordsShowMessage() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "reg-test-user")
                .param("password", "secret")
                .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Passwords do not match")));

        assertThat(userRepository.existsByUsername("reg-test-user")).isFalse();
    }

    @Test
    void duplicateUsernameShowsMessage() throws Exception {
        userRepository.saveAndFlush(new User("reg-test-user", "secret", "USER"));

        mockMvc.perform(post("/register")
                .param("username", "reg-test-user")
                .param("password", "secret")
                .param("confirmPassword", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That username is already taken")));
    }

    @Test
    void blankFieldsRedisplayForm() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "")
                .param("password", "")
                .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("confirmPassword")));
    }
}
