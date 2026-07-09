package com.sincheon90.eventparticipation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Participation System API")
                        .description("イベント参加、重複参加防止、ポイント付与を想定したバックエンドAPI")
                        .version("v1.0.0"));
    }
}
