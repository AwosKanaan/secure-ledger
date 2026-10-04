package org.secureledger.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Secure Ledger API",
                version = "v1",
                description = "Log in via POST /ledger/auth/login, then click Authorize and paste the accessToken. "
                        + "Seed users: gilbert@gmail.com / gilbert123 and bob@ledger.test / Ledger-Test-2026!"),
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_SCHEME))
@SecurityScheme(
        name = SwaggerConfig.BEARER_SCHEME,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class SwaggerConfig {

    public static final String BEARER_SCHEME = "bearerAuth";
}
