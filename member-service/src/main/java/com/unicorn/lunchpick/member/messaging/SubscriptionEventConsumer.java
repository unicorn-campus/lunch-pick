package com.unicorn.lunchpick.member.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;

/**
 * 구독 이벤트 MQ 소비자 — Redis Streams
 *
 * <p>payment-service가 발행하는 구독 이벤트(Redis Streams)를 소비하여
 * 회원의 구독 상태 캐시를 갱신합니다.</p>
 *
 * <p><b>활성화 조건:</b> {@code mq.redis-streams.enabled=true}</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mq.redis-streams.enabled", havingValue = "true", matchIfMissing = false)
public class SubscriptionEventConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final MemberService memberService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String memberId = message.getValue().get("memberId");
            String plan = message.getValue().get("planId");
            String expiresAt = message.getValue().get("expiresAt");

            if (memberId == null || memberId.isBlank()) {
                log.warn("구독 이벤트 필수 필드 누락: memberId — messageId: {}", message.getId());
                return;
            }

            memberService.updateSubscriptionCache(memberId, plan, expiresAt);
            log.info("구독 이벤트 처리 완료 — memberId: {}, plan: {}", memberId, plan);
        } catch (Exception ex) {
            log.error("구독 이벤트 처리 중 오류 — messageId: {}", message.getId(), ex);
        }
    }
}
