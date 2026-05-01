package com.openfinova.banking.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI 3 documentation for the runnable banking application, including OAuth2/OIDC
 * against the embedded authorization server and identity REST APIs under {@code /api/v1/identity}.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openFinovaOpenAPI(
            @Value("${spring.security.oauth2.authorizationserver.issuer:http://localhost:8080}") String issuer) {
        String authUrl = issuer + "/oauth2/authorize";
        String tokenUrl = issuer + "/oauth2/token";

        OAuthFlow authorizationCode = new OAuthFlow().authorizationUrl(authUrl).tokenUrl(tokenUrl).scopes(
                new Scopes().addString("openid", "OpenID Connect").addString("profile", "End-user profile")
                        .addString("banking.staff", "Staff portal API access")
                        .addString("banking.customer", "Customer channel API access"));

        return new OpenAPI()
                .info(
                        new Info().title("OpenFinova Banking API").version("v1")
                                .description(apiDescription(issuer, authUrl, tokenUrl)))
                .components(
                        new Components().addSecuritySchemes(
                                "oauth2",
                                new SecurityScheme().type(SecurityScheme.Type.OAUTH2).description(
                                        "Authorization Code flow with refresh tokens. "
                                                + "Dev clients: staff-app (Swagger redirect registered), customer-app. "
                                                + "JWT access tokens include a `permissions` claim used for "
                                                + "`@PreAuthorize` authorities.")
                                        .flows(new OAuthFlows().authorizationCode(authorizationCode))));
    }

    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder().group("all").pathsToMatch("/**").build();
    }

    @Bean
    public GroupedOpenApi identityApis() {
        return GroupedOpenApi.builder().group("identity").pathsToMatch("/api/v1/identity/**").build();
    }

    private static String apiDescription(String issuer, String authUrl, String tokenUrl) {
        return """
                Banking REST APIs and **identity** administration under `/api/v1/identity`.

                ## OAuth2 / OIDC (authorization server)

                This application hosts a Spring Authorization Server. Use the **Authorize** action in Swagger UI \
                or any OIDC client.

                | Purpose | URL |
                |---------|-----|
                | OIDC discovery | `%s/.well-known/openid-configuration` |
                | Authorize | `%s` |
                | Token | `%s` |
                | JWK set | `%s/oauth2/jwks` |

                Supported grant types (registered clients): **authorization_code**, **refresh_token**. \
                Interactive staff login and optional step-up MFA use browser routes such as `/login`, \
                `/mfa/challenge`, and `POST /mfa/verify` (HTML form: 6-digit TOTP or 8-digit recovery code), \
                not OpenAPI.

                ## TOTP MFA (self-service API)

                Authenticated users with the `mfa:manage:own` permission:

                1. **POST** `/api/v1/identity/me/mfa/setup` — returns TOTP secret, `otpauth` QR URI, and one-time recovery codes.
                2. **POST** `/api/v1/identity/me/mfa/verify` — submit a 6-digit TOTP code to enable MFA.
                3. **DELETE** `/api/v1/identity/me/mfa` — disable MFA (requires current password).

                Store recovery codes securely; they are shown only once at setup.
                """
                .formatted(issuer, authUrl, tokenUrl, issuer);
    }
}
