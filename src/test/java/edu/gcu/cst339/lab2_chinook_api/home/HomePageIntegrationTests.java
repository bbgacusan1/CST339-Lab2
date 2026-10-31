package edu.gcu.cst339.lab2_chinook_api.home;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomePageIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePageRendersHtmlWithBootstrap() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("bootstrap")));
    }

    @Test
    void homePageKeepsSwaggerLink() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("/swagger-ui.html")));
    }

    @Test
    void albumApiStillReturnsJson() throws Exception {
        mockMvc.perform(get("/api/albums/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.albumId").value(1));
    }
}
