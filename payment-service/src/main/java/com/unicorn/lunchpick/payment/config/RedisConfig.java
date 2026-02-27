package com.unicorn.lunchpick.payment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정
 *
 * <p>캐시용 DB(환경변수 REDIS_DATABASE)와 MQ용 DB 0을 분리합니다.</p>
 * <ul>
 *   <li>기본 StringRedisTemplate: 캐시용 (DB 3)</li>
 *   <li>mqRedisTemplate: Redis Streams MQ용 (DB 0 고정)</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public LettuceConnectionFactory mqConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate mqRedisTemplate(@Qualifier("mqConnectionFactory") RedisConnectionFactory mqConnectionFactory) {
        return new StringRedisTemplate(mqConnectionFactory);
    }
}
