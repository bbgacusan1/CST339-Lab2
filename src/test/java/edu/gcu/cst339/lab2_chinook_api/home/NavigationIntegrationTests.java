package edu.gcu.cst339.lab2_chinook_api.home;

import static org.hamcrest.Matchers.containsString;
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

@SpringBootTest
@AutoConfigureMockMvc
class NavigationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    // /admin/users requires login, so run as a mock ADMIN to reach every page.
    // Branch 6 changes which links each role sees; update these assertions then.
    @ParameterizedTest
    @ValueSource(strings = { "/", "/login", "/register", "/admin/users" })
    @WithMockUser(roles = "ADMIN")
    void pageShowsResponsiveNavbar(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("navbar-toggler")))
                .andExpect(content().string(containsString("href=\"/login\"")))
                .andExpect(content().string(containsString("href=\"/register\"")))
                .andExpect(content().string(containsString("href=\"/admin/users\"")));
    }
}