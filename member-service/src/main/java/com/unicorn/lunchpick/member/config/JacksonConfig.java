package com.unicorn.lunchpick.member.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 설정
 *
 * <p>Java 8+ 날짜/시간 타입({@link java.time.LocalDateTime} 등)의 직렬화를 지원합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper 빈 등록
     *
     * <p>JavaTimeModule 등록으로 LocalDateTime 등 JSR-310 타입을 ISO 8601 문자열로 직렬화합니다.
     * WRITE_DATES_AS_TIMESTAMPS 비활성화로 숫자 배열 대신 문자열 형태로 출력합니다.</p>
     *
     * @return 설정된 ObjectMapper 인스턴스
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
