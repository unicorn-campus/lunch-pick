package com.unicorn.lunchpick.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 *
 * <p><b>Redis DB 용도:</b></p>
 * <ul>
 *   <li>DB 2: 추천 결과 캐시, 추천 이유 캐시, Stale 캐시</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate 빈 등록
     *
     * @param redisConnectionFactory Redis 연결 팩토리
     * @return StringRedisTemplate 인스턴스
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}
