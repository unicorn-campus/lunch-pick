package com.unicorn.lunchpick.payment.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 구독 이벤트 발행자 — Redis Streams MQ (DB 0)
 *
 * <p>Redis Streams ({@code XADD})를 사용하여 구독 이벤트를 발행합니다.
 * MQ 전용 RedisTemplate(DB 0)을 사용하여 캐시 DB와 분리합니다.</p>
 *
 * <p><b>이벤트 유형:</b></p>
 * <ul>
 *   <li>{@code SUBSCRIPTION_ACTIVATED} — 구독 결제 성공</li>
 *   <li>{@code SUBSCRIPTION_PENDING_CANCEL} — 해지 예약</li>
 *   <li>{@code SUBSCRIPTION_TRIAL_EXTENDED} — 7일 무료 연장</li>
 * </ul>
 */
@Slf4j
@Component
public class SubscriptionEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SubscriptionEventPublisher(
            @Qualifier("mqRedisTemplate") StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${mq.subscription.topic:subscription-events}")
    private String streamKey;

    public void publishActivated(String memberId, String subscriptionId,
                                  String planId, LocalDateTime expiresAt) {
        publish("SUBSCRIPTION_ACTIVATED", memberId, subscriptionId, planId, expiresAt);
    }

    public void publishPendingCancel(String memberId, String subscriptionId,
                                      String planId, LocalDateTime expiresAt) {
        publish("SUBSCRIPTION_PENDING_CANCEL", memberId, subscriptionId, planId, expiresAt);
    }

    public void publishTrialExtended(String memberId, String subscriptionId,
                                      String planId, LocalDateTime newExpiresAt) {
        publish("SUBSCRIPTION_TRIAL_EXTENDED", memberId, subscriptionId, planId, newExpiresAt);
    }

    private void publish(String eventType, String memberId, String subscriptionId,
                         String planId, LocalDateTime expiresAt) {
        try {
            Map<String, String> fields = Map.of(
                    "eventType", eventType,
                    "memberId", memberId,
                    "subscriptionId", subscriptionId,
                    "planId", planId,
                    "expiresAt", expiresAt != null ? expiresAt.toString() : ""
            );
            StringRecord record = StreamRecords.string(fields).withStreamKey(streamKey);
            redisTemplate.opsForStream().add(record);
            log.info("구독 이벤트 발행 완료 — eventType: {}, memberId: {}", eventType, memberId);
        } catch (Exception ex) {
            log.warn("구독 이벤트 발행 실패 (서비스 계속 동작) — eventType: {}, memberId: {}", eventType, memberId, ex);
        }
    }
}
