package com.unicorn.lunchpick.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정
 *
 * <p>카카오 OAuth2 API 호출에 사용되는 RestTemplate 빈을 등록합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate 빈 등록
     *
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
