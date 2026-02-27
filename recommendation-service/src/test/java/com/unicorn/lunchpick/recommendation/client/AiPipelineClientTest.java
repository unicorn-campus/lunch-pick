package com.unicorn.lunchpick.recommendation.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.spring6.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI Pipeline 클라이언트 통합 테스트
 *
 * <p>WireMock으로 ai-pipeline-service를 모킹하여 정상 응답, 5xx 오류,
 * Circuit Breaker Fallback 동작을 검증합니다.</p>
 *
 * <p>Spring 컨텍스트를 로드하지 않고 WebClient와 Resilience4j를 직접 구성하여
 * DB·Redis 의존성 없이 빠르게 실행됩니다.</p>
 *
 * <p><b>테스트 케이스:</b></p>
 * <ul>
 *   <li>정상 추천 생성 응답 수신</li>
 *   <li>정상 이유 생성 응답 수신</li>
 *   <li>500 오류 시 Fallback 반환</li>
 *   <li>LLM 실패(isReasonReady=false) 응답 처리</li>
 *   <li>이유 생성 500 오류 시 Fallback 반환</li>
 *   <li>AI Pipeline CB Open 폴백 응답 처리</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiPipelineClient
 * @see AiPipelineClientImpl
 */
class AiPipelineClientTest {

    /** WireMock 서버 (동적 포트) */
    private WireMockServer wireMockServer;

    /** 테스트 대상 클라이언트 */
    private AiPipelineClient aiPipelineClient;

    /**
     * 각 테스트 전 WireMock 서버 기동 및 클라이언트 초기화
     *
     * <p>테스트 전용 Circuit Breaker 설정:
     * sliding-window-size=5, failure-rate-threshold=50%, wait-duration=2s</p>
     */
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());

        // 테스트 전용 WebClient — WireMock 서버 포트로 baseUrl 설정
        // connectTimeout 1초, readTimeout 3초 (테스트 속도 우선)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)
                .responseTimeout(Duration.ofSeconds(3))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))
                );

        WebClient testWebClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        // 테스트 전용 Circuit Breaker — sliding-window-size=5, failure-rate-threshold=50%
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(2)
                .minimumNumberOfCalls(3)
                .recordExceptions(Exception.class)
                .build();

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.of(cbConfig);

        // AiPipelineClientImpl을 직접 생성 (Spring 컨텍스트 불필요)
        aiPipelineClient = new AiPipelineClientImpl(testWebClient);
    }

    /**
     * 각 테스트 후 WireMock 서버 종료
     */
    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    // -------------------------------------------------------------------------
    // 추천 생성 테스트
    // -------------------------------------------------------------------------

    /**
     * 정상 케이스: AI Pipeline 추천 생성 성공
     */
    @Test
    @DisplayName("AI Pipeline 추천 생성 - 정상 응답 반환")
    void getRecommendations_success() {
        // given
        stubFor(post(urlEqualTo("/api/v1/ai/recommendations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "recommendations": [
                                    {
                                      "restaurantId": "rest-001",
                                      "restaurantName": "광화문 된장마을",
                                      "representativeMenu": "된장찌개 정식",
                                      "category": "한식",
                                      "reasonSummary": "비 오는 날 따뜻한 한식",
                                      "confidenceScore": 87,
                                      "distanceMeters": 250,
                                      "estimatedWalkMinutes": 3
                                    }
                                  ],
                                  "isFallback": false,
                                  "isColdStart": false,
                                  "coldStartTag": null,
                                  "cacheKey": "rec:member-001:3756:12697",
                                  "cachedUntil": null,
                                  "metadata": {
                                    "source": "LLM",
                                    "modelUsed": "claude-3-5-haiku-20241022",
                                    "latencyMs": 850,
                                    "circuitBreakerState": "CLOSED"
                                  }
                                }
                                """)));

        AiRecommendationRequest request = buildSampleRecommendationRequest("member-001");

        // when
        AiRecommendationResponse response = aiPipelineClient.getRecommendations(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isFallback()).isFalse();
        assertThat(response.isColdStart()).isFalse();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.recommendations().get(0).restaurantId()).isEqualTo("rest-001");
        assertThat(response.recommendations().get(0).restaurantName()).isEqualTo("광화문 된장마을");
        assertThat(response.recommendations().get(0).confidenceScore()).isEqualTo(87);

        verify(1, postRequestedFor(urlEqualTo("/api/v1/ai/recommendations")));
    }

    /**
     * 폴백 케이스: AI Pipeline 500 오류 시 Fallback 응답 반환
     */
    @Test
    @DisplayName("AI Pipeline 추천 생성 - 500 오류 시 Fallback 반환")
    void getRecommendations_serverError_returnsFallback() {
        // given
        stubFor(post(urlEqualTo("/api/v1/ai/recommendations"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        AiRecommendationRequest request = buildSampleRecommendationRequest("member-002");

        // when
        AiRecommendationResponse response = aiPipelineClient.getRecommendations(request);

        // then: Fallback 응답 확인 (빈 목록, isFallback=true)
        assertThat(response).isNotNull();
        assertThat(response.isFallback()).isTrue();
        assertThat(response.recommendations()).isEmpty();
    }

    /**
     * AI Pipeline 폴백 응답(CB Open 상태) 직접 반환 처리
     *
     * <p>AI Pipeline 자체가 CB Open 상태로 isFallback=true를 반환하는 경우.</p>
     */
    @Test
    @DisplayName("AI Pipeline 추천 생성 - AI Pipeline CB Open 폴백 응답 처리")
    void getRecommendations_aiFallbackResponse_handled() {
        // given: AI Pipeline이 규칙 기반 폴백 응답 반환
        stubFor(post(urlEqualTo("/api/v1/ai/recommendations"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "recommendations": [
                                    {
                                      "restaurantId": "rest-fallback-001",
                                      "restaurantName": "주변 인기 식당",
                                      "representativeMenu": "오늘의 메뉴",
                                      "category": "한식",
                                      "reasonSummary": "주변 인기 식당이에요",
                                      "confidenceScore": 60,
                                      "distanceMeters": 300,
                                      "estimatedWalkMinutes": 4
                                    }
                                  ],
                                  "isFallback": true,
                                  "isColdStart": false,
                                  "coldStartTag": null,
                                  "cacheKey": "rec:member-004:3756:12697",
                                  "cachedUntil": null,
                                  "metadata": {
                                    "source": "FALLBACK_RULE_BASED",
                                    "modelUsed": null,
                                    "latencyMs": 50,
                                    "circuitBreakerState": "OPEN"
                                  }
                                }
                                """)));

        AiRecommendationRequest request = buildSampleRecommendationRequest("member-004");

        // when
        AiRecommendationResponse response = aiPipelineClient.getRecommendations(request);

        // then: 폴백 응답이지만 추천 목록은 있음
        assertThat(response).isNotNull();
        assertThat(response.isFallback()).isTrue();
        assertThat(response.recommendations()).hasSize(1);
        assertThat(response.metadata().source()).isEqualTo("FALLBACK_RULE_BASED");
    }

    // -------------------------------------------------------------------------
    // 추천 이유 생성 테스트
    // -------------------------------------------------------------------------

    /**
     * 정상 케이스: AI Pipeline 이유 생성 성공
     */
    @Test
    @DisplayName("AI Pipeline 이유 생성 - 정상 응답 반환")
    void getRecommendationReason_success() {
        // given
        stubFor(post(urlEqualTo("/api/v1/ai/recommendation-reason"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "recommendationId": "rec-abc-123",
                                  "naturalLanguageReason": "비 오는 날 + 어제 양식 → 따뜻한 한식을 추천드려요",
                                  "confidenceScore": 87,
                                  "contextTags": ["날씨", "이력"],
                                  "isReasonReady": true,
                                  "fallbackReason": null,
                                  "cachedUntil": null,
                                  "metadata": {
                                    "source": "LLM",
                                    "modelUsed": "claude-3-5-haiku-20241022",
                                    "latencyMs": 420,
                                    "circuitBreakerState": "CLOSED"
                                  }
                                }
                                """)));

        AiReasonRequest request = buildSampleReasonRequest("rec-abc-123");

        // when
        AiReasonResponse response = aiPipelineClient.getRecommendationReason(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isReasonReady()).isTrue();
        assertThat(response.recommendationId()).isEqualTo("rec-abc-123");
        assertThat(response.naturalLanguageReason())
                .isEqualTo("비 오는 날 + 어제 양식 → 따뜻한 한식을 추천드려요");
        assertThat(response.contextTags()).containsExactlyInAnyOrder("날씨", "이력");
        assertThat(response.confidenceScore()).isEqualTo(87);

        verify(1, postRequestedFor(urlEqualTo("/api/v1/ai/recommendation-reason")));
    }

    /**
     * LLM 실패 케이스: AI Pipeline이 isReasonReady=false 반환 (200 응답)
     */
    @Test
    @DisplayName("AI Pipeline 이유 생성 - LLM 실패 시 isReasonReady=false 응답 처리")
    void getRecommendationReason_llmFailure_isReasonReadyFalse() {
        // given
        stubFor(post(urlEqualTo("/api/v1/ai/recommendation-reason"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "recommendationId": "rec-xyz-456",
                                  "naturalLanguageReason": "거리 250m, 평점 4.3",
                                  "confidenceScore": 0,
                                  "contextTags": [],
                                  "isReasonReady": false,
                                  "fallbackReason": "추천 이유를 준비 중이에요.",
                                  "cachedUntil": null,
                                  "metadata": {
                                    "source": "LLM",
                                    "modelUsed": null,
                                    "latencyMs": 100,
                                    "circuitBreakerState": "CLOSED"
                                  }
                                }
                                """)));

        AiReasonRequest request = buildSampleReasonRequest("rec-xyz-456");

        // when
        AiReasonResponse response = aiPipelineClient.getRecommendationReason(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isReasonReady()).isFalse();
        assertThat(response.fallbackReason()).isEqualTo("추천 이유를 준비 중이에요.");
        assertThat(response.recommendationId()).isEqualTo("rec-xyz-456");
    }

    /**
     * 폴백 케이스: AI Pipeline 이유 생성 500 오류 시 Fallback 반환
     */
    @Test
    @DisplayName("AI Pipeline 이유 생성 - 500 오류 시 Fallback 반환")
    void getRecommendationReason_serverError_returnsFallback() {
        // given
        stubFor(post(urlEqualTo("/api/v1/ai/recommendation-reason"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        AiReasonRequest request = buildSampleReasonRequest("rec-err-789");

        // when
        AiReasonResponse response = aiPipelineClient.getRecommendationReason(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isReasonReady()).isFalse();
        assertThat(response.recommendationId()).isEqualTo("rec-err-789");
        assertThat(response.fallbackReason()).isNotBlank();
    }

    // -------------------------------------------------------------------------
    // 헬퍼 메서드
    // -------------------------------------------------------------------------

    /**
     * 테스트용 추천 생성 요청 빌드
     *
     * @param memberId 회원 식별자
     * @return 추천 생성 요청
     */
    private AiRecommendationRequest buildSampleRecommendationRequest(String memberId) {
        return AiRecommendationRequest.builder()
                .memberId(memberId)
                .latitude(37.5665)
                .longitude(126.9780)
                .requestedAt(Instant.now())
                .isColdStart(false)
                .feedbackCount(10)
                .allergenFilter(List.of())
                .recentMealHistory(List.of())
                .excludeRestaurantIds(List.of())
                .build();
    }

    /**
     * 테스트용 이유 생성 요청 빌드
     *
     * @param recommendationId 추천 식별자
     * @return 이유 생성 요청
     */
    private AiReasonRequest buildSampleReasonRequest(String recommendationId) {
        return AiReasonRequest.builder()
                .recommendationId(recommendationId)
                .restaurantId("rest-001")
                .restaurantName("광화문 된장마을")
                .category("한식")
                .memberId("member-001")
                .confidenceScore(87)
                .representativeMenu("된장찌개 정식")
                .build();
    }
}
