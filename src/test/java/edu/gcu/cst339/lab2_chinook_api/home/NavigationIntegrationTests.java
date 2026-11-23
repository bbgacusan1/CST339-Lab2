package edu.gcu.cst339.lab2_chinook_api.home;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The navbar uses sec:authorize, so each role must see a different set of links.
@SpringBootTest
@AutoConfigureMockMvc
class NavigationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    // Anonymous visitors: Login and Register only, nothing that needs an account.
    @ParameterizedTest
    @ValueSource(strings = { "/", "/login", "/register" })
    void anonymousVisitorSeesLoginAndRegisterOnly(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("navbar-toggler")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(not(containsString("href=\"/admin/users\""))))
                .andExpect(content().string(not(containsString("href=\"/change-password\""))))
                .andExpect(content().string(not(containsString("action=\"/logout\""))));
    }

    // A regular USER: account links and their username, but no admin link.
    @Test
    @WithMockUser(username = "nav-user", roles = "USER")
    void userSeesAccountLinksButNotAdminLink() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nav-user")))
                .andExpect(content().string(containsString("href=\"/change-password\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(not(containsString("href=\"/admin/users\""))))
                .andExpect(content().string(not(containsString("href=\"/login\""))))
                .andExpect(content().string(not(containsString("href=\"/register\""))));
    }

    // An ADMIN: everything a USER sees plus User Administration.
    @ParameterizedTest
    @ValueSource(strings = { "/", "/admin/users" })
    @WithMockUser(username = "nav-admin", roles = "ADMIN")
    void adminSeesAdminLink(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nav-admin")))
                .andExpect(content().string(containsString("href=\"/admin/users\"")))
                .andExpect(content().string(containsString("href=\"/change-password\"")))
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(not(containsString("href=\"/login\""))))
                .andExpect(content().string(not(containsString("href=\"/register\""))));
    }
}
