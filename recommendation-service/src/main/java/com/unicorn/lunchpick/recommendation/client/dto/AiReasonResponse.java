package com.unicorn.lunchpick.recommendation.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI Pipeline 추천 이유 생성 응답 DTO
 *
 * <p>AI Pipeline {@code POST /api/v1/ai/recommendation-reason} 응답 본문입니다.
 * Python Pydantic 모델 {@code AiReasonResponse}와 필드명을 맞춥니다.</p>
 *
 * <p>LLM 실패 시에도 AI Pipeline은 200을 반환하며,
 * {@code isReasonReady=false}, {@code fallbackReason}에 기본 이유가 포함됩니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see AiReasonRequest
 */
public record AiReasonResponse(

        /** 추천 식별자 */
        @JsonProperty("recommendationId")
        String recommendationId,

        /** 자연어 추천 이유 (50자 이내) */
        @JsonProperty("naturalLanguageReason")
        String naturalLanguageReason,

        /** 확신 스코어 (0~100) */
        @JsonProperty("confidenceScore")
        int confidenceScore,

        /** 컨텍스트 태그 목록 (날씨, 이력, 취향, 요일, 시간) */
        @JsonProperty("contextTags")
        List<String> contextTags,

        /** 이유 생성 성공 여부 (LLM 실패 시 false) */
        @JsonProperty("isReasonReady")
        boolean isReasonReady,

        /** 폴백 이유 (LLM 실패 시 기본값 제공) */
        @JsonProperty("fallbackReason")
        String fallbackReason,

        /** 캐시 만료 시각 (ISO-8601) */
        @JsonProperty("cachedUntil")
        String cachedUntil,

        /** AI 메타데이터 */
        @JsonProperty("metadata")
        AiRecommendationResponse.AiMetadata metadata
) {}
