package edu.gcu.cst339.lab2_chinook_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
class OpenApiConfig {

    // Name used to link the scheme definition to the requirement below.
    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    OpenAPI chinookOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chinook Album API")
                        .description("CST-339 Lab 5 - CRUD API for Chinook albums secured with Spring Security. "
                                + "GET requires the USER or ADMIN role; POST, PUT and DELETE require ADMIN. "
                                + "Click Authorize and log in with HTTP Basic to try the endpoints.")
                        .version("1.0"))
                // Defines HTTP Basic as a login option, which adds the Authorize button to Swagger UI.
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                // Applies HTTP Basic to every endpoint, so each shows a lock and sends the credentials.
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
