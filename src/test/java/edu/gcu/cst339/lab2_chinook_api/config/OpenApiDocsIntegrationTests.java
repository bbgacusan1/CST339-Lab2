package edu.gcu.cst339.lab2_chinook_api.config;

import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// Branch 8: the OpenAPI docs describe HTTP Basic so Swagger UI shows an Authorize button.
// No mock user here on purpose: the docs themselves must stay public.
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void docsDefineHttpBasicSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
    }

    @Test
    void basicAuthAppliesToEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.security[0]", hasKey("basicAuth")));
    }

    @Test
    void writeEndpointsDocumentUnauthorizedAndForbidden() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/albums'].post.responses", hasKey("401")))
                .andExpect(jsonPath("$.paths['/api/albums'].post.responses", hasKey("403")))
                .andExpect(jsonPath("$.paths['/api/albums/{id}'].delete.responses", hasKey("403")));
    }
}
