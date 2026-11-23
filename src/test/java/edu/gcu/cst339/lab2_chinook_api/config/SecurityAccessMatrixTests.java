package edu.gcu.cst339.lab2_chinook_api.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * The whole access-control picture from SecurityConfig in one place.
 * Each row is: HTTP method, path, expected status, and (optionally) where a redirect should go.
 *
 * Browser pages are requested with Accept: text/html (so anonymous users get a 302 to /login);
 * /api requests use JSON (so anonymous users get a 401 Basic challenge, like Postman).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityAccessMatrixTests {

    private static final String ALBUM_JSON = """
            {"title": "Matrix Test Album", "artistId": 1}
            """;

    @Autowired
    private MockMvc mockMvc;

    private ResultActions perform(String method, String path) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), path);
        if (path.startsWith("/api")) {
            builder.accept(MediaType.APPLICATION_JSON);
            if (method.equals("POST") || method.equals("PUT")) {
                builder.contentType(MediaType.APPLICATION_JSON).content(ALBUM_JSON);
            }
        } else if (path.startsWith("/v3/api-docs")) {
            // The OpenAPI document is JSON only; asking for HTML gives 406 Not Acceptable.
            builder.accept(MediaType.APPLICATION_JSON);
        } else {
            builder.accept(MediaType.TEXT_HTML);
        }
        return mockMvc.perform(builder);
    }

    private void check(String method, String path, int expectedStatus, String redirect) throws Exception {
        ResultActions result = perform(method, path).andExpect(status().is(expectedStatus));
        if (redirect != null) {
            result.andExpect(header().string("Location", containsString(redirect)));
        }
    }

    @ParameterizedTest(name = "anonymous {0} {1} -> {2}")
    @CsvSource({
            // Public pages and docs
            "GET,    /,                      200,",
            "GET,    /login,                 200,",
            "GET,    /register,              200,",
            "GET,    /swagger-ui.html,       302, /swagger-ui/index.html",
            "GET,    /v3/api-docs,           200,",
            // Pages that need login: redirect to the login form
            "GET,    /change-password,       302, /login",
            "GET,    /admin/users,           302, /login",
            // REST API: 401 Basic challenge for every method
            "GET,    /api/albums,            401,",
            "GET,    /api/albums/1,          401,",
            "POST,   /api/albums,            401,",
            "PUT,    /api/albums/1,          401,",
            "DELETE, /api/albums/1,          401,"
    })
    void anonymousAccess(String method, String path, int expectedStatus, String redirect) throws Exception {
        check(method, path, expectedStatus, redirect);
    }

    @ParameterizedTest(name = "USER {0} {1} -> {2}")
    @CsvSource({
            "GET,    /,                      200",
            "GET,    /change-password,       200",
            // Admin pages are ADMIN only
            "GET,    /admin/users,           403",
            // REST API: read only
            "GET,    /api/albums,            200",
            "GET,    /api/albums/1,          200",
            "POST,   /api/albums,            403",
            "PUT,    /api/albums/1,          403",
            "DELETE, /api/albums/1,          403"
    })
    @WithMockUser(username = "matrix-user", roles = "USER")
    void userAccess(String method, String path, int expectedStatus) throws Exception {
        check(method, path, expectedStatus, null);
    }

    // Changes made here roll back (@Transactional), so writing to album 1 is safe.
    @ParameterizedTest(name = "ADMIN {0} {1} -> {2}")
    @CsvSource({
            "GET,    /,                      200",
            "GET,    /change-password,       200",
            "GET,    /admin/users,           200",
            // REST API: full CRUD
            "GET,    /api/albums,            200",
            "GET,    /api/albums/1,          200",
            "POST,   /api/albums,            201",
            "PUT,    /api/albums/1,          200",
            // 404 means the request got past security and reached the controller (no such album)
            "DELETE, /api/albums/999999,     404"
    })
    @WithMockUser(username = "matrix-admin", roles = "ADMIN")
    void adminAccess(String method, String path, int expectedStatus) throws Exception {
        check(method, path, expectedStatus, null);
    }
}
