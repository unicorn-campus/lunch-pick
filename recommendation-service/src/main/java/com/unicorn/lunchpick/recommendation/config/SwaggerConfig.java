package com.unicorn.lunchpick.recommendation.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 설정
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class SwaggerConfig {

    /**
     * OpenAPI 빈 등록
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("런치픽 추천·이력 서비스 API")
                        .description("오늘의 추천, 식사 기록, 피드백, 이력 타임라인, 취향 인사이트 API")
                        .version("1.0.0")
                        .contact(new Contact().name("lunchpick-team")))
                .addServersItem(new Server().url("http://localhost:8082").description("로컬 개발 서버"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 액세스 토큰 (만료 1시간)")));
    }
}
