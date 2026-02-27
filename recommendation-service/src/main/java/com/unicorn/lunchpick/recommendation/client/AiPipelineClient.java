package com.unicorn.lunchpick.recommendation.client;

import com.unicorn.lunchpick.recommendation.client.dto.AiInsightRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiInsightResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiReasonResponse;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationRequest;
import com.unicorn.lunchpick.recommendation.client.dto.AiRecommendationResponse;

/**
 * AI Pipeline 서비스 클라이언트 인터페이스
 *
 * <p>recommendation-service에서 ai-pipeline-service(포트 8084)를 호출하는 계약을 정의합니다.</p>
 *
 * <p><b>제공 기능:</b></p>
 * <ul>
 *   <li>AI 추천 생성 — {@code POST /api/v1/ai/recommendations}</li>
 *   <li>추천 이유 생성 — {@code POST /api/v1/ai/recommendation-reason}</li>
 * </ul>
 *
 * <p><b>장애 격리:</b> 구현체에서 Resilience4j Circuit Breaker를 적용합니다.
 * CB Open 시 Fallback 메서드가 호출되어 규칙 기반 기본 응답을 반환합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiPipelineClientImpl
 */
public interface AiPipelineClient {

    /**
     * AI 추천 생성 요청
     *
     * <p>캐시 미스 시 호출. LLM 기반 추천 3개를 생성하여 반환합니다.
     * Circuit Breaker Open 또는 장애 시 폴백 응답({@code isFallback=true})이 반환됩니다.</p>
     *
     * @param request 추천 생성 요청 (취향 벡터, 위치, 날씨, 이력 포함)
     * @return AI 추천 응답 (정상 또는 폴백)
     */
    AiRecommendationResponse getRecommendations(AiRecommendationRequest request);

    /**
     * 추천 이유 생성 요청
     *
     * <p>추천 이유 상세 조회 시 호출. LLM이 자연어 이유 문장을 생성합니다.
     * LLM 실패 시에도 AI Pipeline이 200을 반환하며 {@code isReasonReady=false}로 표시됩니다.</p>
     *
     * @param request 이유 생성 요청 (식당 정보, 취향, 날씨, 이력 포함)
     * @return 추천 이유 응답 (자연어 또는 폴백 이유)
     */
    AiReasonResponse getRecommendationReason(AiReasonRequest request);

    /**
     * AI 인사이트 분석 요청
     *
     * <p>식사 기록 기반 AI 인사이트(주간 리포트, 밸런스 진단, 만족도 패턴)를 생성합니다.
     * LLM 장애 시에도 AI Pipeline이 200을 반환하며 규칙 기반 폴백 인사이트가 포함됩니다.</p>
     *
     * @param request 인사이트 분석 요청 (식사 기록, 카테고리 분포 포함)
     * @return AI 인사이트 응답 (정상 또는 폴백)
     */
    AiInsightResponse getInsightAnalysis(AiInsightRequest request);
}
