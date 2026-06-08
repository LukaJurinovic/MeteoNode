package com.example.meteonode.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "MeteoNode API",
                description = "Environmental monitoring — weather stations, IoT nodes, sensor measurements, alarm rules",
                version     = "1.0.0"
        )
)
@SecuritySchemes({
        @SecurityScheme(
                name   = "bearerAuth",
                type   = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT"
        ),
        @SecurityScheme(
                name   = "apiKey",
                type   = SecuritySchemeType.APIKEY,
                in     = SecuritySchemeIn.HEADER,
                paramName = "X-Api-Key"
        )
})
public class OpenApiConfig {}
