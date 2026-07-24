package com.skillsphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API once for Swagger UI instead of documenting each endpoint by hand elsewhere.
 *
 * The bearer scheme lets a student paste a JWT into Swagger's Authorize dialog and test protected
 * endpoints. It is intentionally standard HTTP bearer authentication rather than a custom scheme.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skillSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillSphere API")
                        .version("v1")
                        .description("REST API for the SkillSphere student skill exchange platform."))
                .components(new Components().addSecuritySchemes(
                        "bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
