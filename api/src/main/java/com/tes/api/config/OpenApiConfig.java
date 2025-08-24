package com.tes.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Through Every Season API",
                version = "0.1.0",
                description = "REST API that returns a minimal sufficient set of episodes per season with reasons.",
                contact = @Contact(name = "TES", url = "https://github.com/ArtyemTs/tes")
        ),
        servers = {@Server(url = "/")}
)
public class OpenApiConfig {
}
