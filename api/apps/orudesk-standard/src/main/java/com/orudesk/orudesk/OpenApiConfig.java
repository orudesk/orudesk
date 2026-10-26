package com.orudesk.orudesk;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The header of the OpenAPI contract at /v3/api-docs: which API, which version, which licence, and how to log in.
 * The web app generates its TypeScript client from this contract, so it's part of the product, not decoration.
 * It lives in the app, because the title and version belong to the edition (orudesk-ngo will have its own).
 */
@Configuration
class OpenApiConfig {

    static final String BEARER = "bearer";
    static final String SOURCE = "https://github.com/orudesk/orudesk";

    @Bean
    OpenAPI orudeskOpenApi(ObjectProvider<BuildProperties> build) {
        String version = build.getIfAvailable() == null ? "dev" : build.getIfAvailable().getVersion();
        return new OpenAPI()
                .info(new Info()
                        .title("Orudesk Standard API")
                        .version(version)
                        .description("Open source ERP. Log in with POST /api/v1/auth/login, then send "
                                + "\"Authorization: Bearer <accessToken>\".")
                        .license(new License().name("AGPL-3.0-only").identifier("AGPL-3.0-only")))
                // AGPL section 13: people who use the software over a network must be offered its source
                .externalDocs(new ExternalDocumentation().description("Source code").url(SOURCE))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                // Every endpoint needs the token, unless it says otherwise (@SecurityRequirements, empty)
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
