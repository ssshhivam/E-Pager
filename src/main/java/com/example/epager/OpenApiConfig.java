package com.example.epager;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI epagerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ePager API")
                        .version("0.0.1")
                        .description("Alert ingestion, incident lifecycle, and escalation matrix APIs."));
    }
}
