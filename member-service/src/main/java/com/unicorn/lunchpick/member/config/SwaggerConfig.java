package com.unicorn.lunchpick.member.config;

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
 * <p>런치픽 회원 서비스 API 문서화 설정입니다.</p>
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
     * <p>Bearer JWT 인증 스킴 및 서버 URL 설정을 포함합니다.</p>
     *
     * @return OpenAPI 설정 객체
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .addServersItem(new Server()
                        .url("http://localhost:8081")
                        .description("로컬 개발 서버"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createBearerScheme()));
    }

    /**
     * API 기본 정보 설정
     *
     * @return Info 객체
     */
    private Info apiInfo() {
        return new Info()
                .title("런치픽 회원 서비스 API")
                .description("카카오 소셜 로그인, 취향 온보딩, 위치 동의, 식이제한 설정, 프로필 관리 API")
                .version("1.0.0")
                .contact(new Contact()
                        .name("lunchpick-team"));
    }

    /**
     * Bearer JWT 보안 스킴 생성
     *
     * @return SecurityScheme 객체
     */
    private SecurityScheme createBearerScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT 액세스 토큰 (만료 1시간)");
    }
}
