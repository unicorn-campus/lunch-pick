package com.unicorn.lunchpick.member.config;

import com.unicorn.lunchpick.member.messaging.SubscriptionEventConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;

/**
 * Redis 설정
 *
 * <p>회원 서비스 Redis 연결 및 RedisTemplate 설정입니다.</p>
 *
 * <p><b>Redis DB 용도:</b></p>
 * <ul>
 *   <li>DB 0: 세션({@code session:{member_id}}), JWT 블랙리스트({@code jwt:blacklist:{jti}}),
 *       구독 이벤트 Streams({@code subscription-events})</li>
 *   <li>DB 1: 취향 프로파일 캐시({@code member:taste_profile:{member_id}}), 회원 프로파일 캐시</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${mq.subscription.topic:subscription-events}")
    private String subscriptionStreamKey;

    @Value("${mq.consumer-group:member-service-group}")
    private String consumerGroup;

    @Value("${mq.consumer-name:member-service-1}")
    private String consumerName;

    /**
     * StringRedisTemplate 빈 등록 (캐시용 — 환경변수 REDIS_DATABASE에 따른 DB 사용)
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

    /**
     * MQ 전용 ConnectionFactory (DB 0 고정)
     *
     * <p>Redis Streams MQ는 payment-service와 동일한 DB 0을 사용해야 합니다.
     * 캐시 DB(환경변수 REDIS_DATABASE)와 분리합니다.</p>
     */
    @Bean
    public LettuceConnectionFactory mqConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(0);
        return new LettuceConnectionFactory(config);
    }

    /**
     * MQ 전용 StringRedisTemplate (DB 0 — Consumer Group 생성용)
     */
    @Bean
    public StringRedisTemplate mqRedisTemplate(@Qualifier("mqConnectionFactory") RedisConnectionFactory mqConnectionFactory) {
        return new StringRedisTemplate(mqConnectionFactory);
    }

    /**
     * Redis Streams 메시지 리스너 컨테이너 (MQ 전용 DB 0 사용)
     *
     * <p>{@code mq.redis-streams.enabled=true} 일 때만 활성화됩니다.
     * Consumer Group 방식으로 구독 이벤트를 소비합니다.</p>
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnProperty(name = "mq.redis-streams.enabled", havingValue = "true", matchIfMissing = false)
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            @Qualifier("mqConnectionFactory") RedisConnectionFactory mqConnectionFactory) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(100))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(mqConnectionFactory, options);
        container.start();
        log.info("Redis Streams 리스너 컨테이너 시작 (DB 0) — stream: {}", subscriptionStreamKey);
        return container;
    }

    /**
     * Redis Streams 구독 등록 (MQ 전용 RedisTemplate 사용)
     */
    @Bean
    @ConditionalOnProperty(name = "mq.redis-streams.enabled", havingValue = "true", matchIfMissing = false)
    public Subscription subscriptionEventSubscription(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            SubscriptionEventConsumer consumer,
            @Qualifier("mqRedisTemplate") StringRedisTemplate mqRedisTemplate) {

        ensureConsumerGroup(mqRedisTemplate);

        Subscription subscription = container.receive(
                Consumer.from(consumerGroup, consumerName),
                StreamOffset.create(subscriptionStreamKey, ReadOffset.lastConsumed()),
                consumer);

        log.info("Redis Streams 구독 등록 완료 — stream: {}, group: {}, consumer: {}",
                subscriptionStreamKey, consumerGroup, consumerName);
        return subscription;
    }

    /**
     * Consumer Group이 없으면 생성 (MKSTREAM 옵션으로 Stream 자동 생성)
     */
    private void ensureConsumerGroup(StringRedisTemplate redisTemplate) {
        try {
            redisTemplate.opsForStream()
                    .createGroup(subscriptionStreamKey, ReadOffset.from("0"), consumerGroup);
            log.info("Consumer Group 생성 완료 (DB 0) — stream: {}, group: {}", subscriptionStreamKey, consumerGroup);
        } catch (Exception ex) {
            // 이미 존재하는 Consumer Group이면 무시
            log.debug("Consumer Group 이미 존재 (정상) — stream: {}, group: {}", subscriptionStreamKey, consumerGroup);
        }
    }
}
