package com.unicorn.lunchpick.recommendation.client;

import com.unicorn.lunchpick.recommendation.client.dto.AiReasonRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.time.Duration;
import java.util.List;

/**
 * AI Pipeline 서비스 WebClient 구현체
 *
 * <p>{@link WebClient}를 사용하여 ai-pipeline-service의 HTTP 엔드포인트를 동기 방식으로 호출합니다.</p>
 *
 * <p><b>장애 격리 전략 (2-tier):</b></p>
 * <ol>
 *   <li>내부 try-catch: HTTP 4xx/5xx 및 네트워크 오류 시 즉시 폴백 반환</li>
 *   <li>Resilience4j {@code @CircuitBreaker}: Spring AOP 프록시 환경에서 연속 실패 임계값
 *       초과 시 회로 차단 + 폴백 메서드 호출 (application.yml {@code ai-pipeline} 인스턴스 설정)</li>
 * </ol>
 *
 * <p><b>타임아웃:</b> connectTimeout 5초, readTimeout 30초 ({@code WebClientConfig} 설정 참조)</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiPipelineClient
 * @see com.unicorn.lunchpick.recommendation.config.WebClientConfig
 */
@Slf4j
@Component
public class AiPipelineClientImpl implements AiPipelineClient {

    /** AI 추천 생성 엔드포인트 경로 */
    private static final String RECOMMENDATIONS_PATH = "/api/v1/ai/recommendations";

    /** 추천 이유 생성 엔드포인트 경로 */
    private static final String REASON_PATH = "/api/v1/ai/recommendation-reason";

    /** WebClient 블로킹 타임아웃 — readTimeout(30s)보다 5초 여유 */
    private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(35);

    private final WebClient aiPipelineWebClient;

    /**
     * 생성자 — ai-pipeline-service 전용 WebClient 주입
     *
     * @param aiPipelineWebClient ai-pipeline-service 전용 WebClient 빈
     */
    public AiPipelineClientImpl(@Qualifier("aiPipelineWebClient") WebClient aiPipelineWebClient) {
        this.aiPipelineWebClient = aiPipelineWebClient;
    }

    /**
     * {@inheritDoc}
     *
     * <p>내부 try-catch로 모든 HTTP/네트워크 오류를 폴백으로 처리합니다.
     * Spring AOP 환경에서는 {@code @CircuitBreaker}가 추가적인 회로 차단을 수행합니다.</p>
     *
     * @param request 추천 생성 요청
     * @return AI 추천 응답 (정상 또는 폴백)
     */
    @Override
    @CircuitBreaker(name = "ai-pipeline", fallbackMethod = "getRecommendationsFallback")
    public AiRecommendationResponse getRecommendations(AiRecommendationRequest request) {
        log.debug("[AI Pipeline] 추천 생성 요청 — memberId: {}, isColdStart: {}",
                request.memberId(), request.isColdStart());
        try {
            AiRecommendationResponse response = aiPipelineWebClient.post()
                    .uri(RECOMMENDATIONS_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.warn("[AI Pipeline] 클라이언트 오류 — status: {}", clientResponse.statusCode());
                        return clientResponse.createException();
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.warn("[AI Pipeline] 서버 오류 — status: {}", clientResponse.statusCode());
                        return clientResponse.createException();
                    })
                    .bodyToMono(AiRecommendationResponse.class)
                    .block(BLOCK_TIMEOUT);

            log.debug("[AI Pipeline] 추천 생성 성공 — memberId: {}, isFallback: {}",
                    request.memberId(), response != null && response.isFallback());
            return response;

        } catch (WebClientException | IllegalStateException ex) {
            return getRecommendationsFallback(request, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>내부 try-catch로 모든 HTTP/네트워크 오류를 폴백으로 처리합니다.
     * LLM 실패는 AI Pipeline이 200 + {@code isReasonReady=false}로 반환하므로
     * CB는 네트워크/서버 오류에만 동작합니다.</p>
     *
     * @param request 이유 생성 요청
     * @return 추천 이유 응답 (정상 또는 폴백)
     */
    @Override
    @CircuitBreaker(name = "ai-pipeline", fallbackMethod = "getRecommendationReasonFallback")
    public AiReasonResponse getRecommendationReason(AiReasonRequest request) {
        log.debug("[AI Pipeline] 이유 생성 요청 — recommendationId: {}", request.recommendationId());
        try {
            AiReasonResponse response = aiPipelineWebClient.post()
                    .uri(REASON_PATH)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.warn("[AI Pipeline] 이유 생성 클라이언트 오류 — status: {}", clientResponse.statusCode());
                        return clientResponse.createException();
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                        log.warn("[AI Pipeline] 이유 생성 서버 오류 — status: {}", clientResponse.statusCode());
                        return clientResponse.createException();
                    })
                    .bodyToMono(AiReasonResponse.class)
                    .block(BLOCK_TIMEOUT);

            log.debug("[AI Pipeline] 이유 생성 성공 — recommendationId: {}, isReasonReady: {}",
                    request.recommendationId(), response != null && response.isReasonReady());
            return response;

        } catch (WebClientException | IllegalStateException ex) {
            return getRecommendationReasonFallback(request, ex);
        }
    }

    /**
     * 추천 생성 Fallback — AI Pipeline 장애 시 빈 응답 반환
     *
     * <p>내부 try-catch 또는 Circuit Breaker Open 시 호출됩니다.
     * 빈 추천 목록과 {@code isFallback=true}를 반환하여
     * 상위 서비스에서 규칙 기반 폴백으로 대체합니다.</p>
     *
     * @param request 원본 추천 요청
     * @param t       발생한 예외
     * @return 폴백 추천 응답 (빈 목록, isFallback=true)
     */
    public AiRecommendationResponse getRecommendationsFallback(AiRecommendationRequest request, Throwable t) {
        log.warn("[AI Pipeline Fallback] 추천 생성 실패 — memberId: {}, cause: {}",
                request.memberId(), t.getMessage());
        return new AiRecommendationResponse(
                List.of(),
                true,
                false,
                null,
                "",
                null,
                null
        );
    }

    /**
     * 추천 이유 Fallback — AI Pipeline 장애 시 기본 이유 반환
     *
     * <p>내부 try-catch 또는 Circuit Breaker Open 시 호출됩니다.
     * {@code isReasonReady=false}와 기본 폴백 이유 문자열을 반환합니다.</p>
     *
     * @param request 원본 이유 요청
     * @param t       발생한 예외
     * @return 폴백 이유 응답 (isReasonReady=false)
     */
    public AiReasonResponse getRecommendationReasonFallback(AiReasonRequest request, Throwable t) {
        log.warn("[AI Pipeline Fallback] 이유 생성 실패 — recommendationId: {}, cause: {}",
                request.recommendationId(), t.getMessage());
        return new AiReasonResponse(
                request.recommendationId(),
                "거리 및 평점 기반 추천",
                0,
                List.of(),
                false,
                "추천 이유를 준비 중이에요.",
                null,
                null
        );
    }
}
