package com.unicorn.lunchpick.payment.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI (Swagger UI) 설정
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "결제 서비스 API",
                version = "1.0.0",
                description = "런치픽 결제 서비스 API — 구독 플랜 조회, 구독 결제, 구독 해지"
        )
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT 액세스 토큰"
)
public class SwaggerConfig {
}
