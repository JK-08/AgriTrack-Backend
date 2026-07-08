package AgriTrackBackend.CONFIG;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html — OpenAPI JSON: /v3/api-docs
 * Every endpoint is documented automatically from controller method
 * signatures + DTO field types; @Operation/@ApiResponse annotations on
 * individual controllers (see AuthController, UserController,
 * CustomerController for examples) add request/response examples on top
 * of that baseline — the same pattern extends to any other controller.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI agriTrackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgriTrack API")
                        .description("Tractor-rental platform API — owners, customers, and drivers. "
                                + "Most endpoints require a Bearer access token obtained from "
                                + "/api/v1/user/login (or /mpin/login, /google/login), refreshed via "
                                + "/api/v1/auth/refresh.")
                        .version("v1")
                        .contact(new Contact().name("AgriTrack Backend")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
