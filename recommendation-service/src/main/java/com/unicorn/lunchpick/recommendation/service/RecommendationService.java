package com.unicorn.lunchpick.recommendation.service;

import com.unicorn.lunchpick.recommendation.dto.request.AcceptRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RefreshRecommendationsRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RejectRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.response.AcceptRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RecommendationReasonResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RejectRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.TodayRecommendationsResponse;

/**
 * 추천 서비스 인터페이스
 *
 * <p>오늘의 추천 조회, 추천 이유 조회, 수락/거절/새로고침을 담당합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface RecommendationService {

    /**
     * 오늘의 추천 3개 조회
     *
     * <p>캐시 히트 시 Redis에서 반환, 미스 시 AI Pipeline 호출(Resilience4j CB 보호).
     * AI Pipeline 장애 시 규칙 기반 폴백 추천을 반환합니다.</p>
     *
     * @param memberId  회원 식별자
     * @param latitude  위도
     * @param longitude 경도
     * @return 오늘의 추천 응답
     */
    TodayRecommendationsResponse getTodayRecommendations(String memberId, double latitude, double longitude);

    /**
     * 추천 이유 상세 조회
     *
     * @param memberId         회원 식별자
     * @param recommendationId 추천 ID
     * @return 추천 이유 응답
     */
    RecommendationReasonResponse getRecommendationReason(String memberId, String recommendationId);

    /**
     * 추천 수락
     *
     * @param memberId         회원 식별자
     * @param recommendationId 추천 ID
     * @param request          수락 요청
     * @return 수락 응답
     */
    AcceptRecommendationResponse acceptRecommendation(String memberId, String recommendationId,
                                                       AcceptRecommendationRequest request);

    /**
     * 추천 거절 및 대체 추천 반환
     *
     * @param memberId         회원 식별자
     * @param recommendationId 추천 ID
     * @param request          거절 요청
     * @return 거절 응답 (대체 추천 포함 또는 없음)
     */
    RejectRecommendationResponse rejectRecommendation(String memberId, String recommendationId,
                                                       RejectRecommendationRequest request);

    /**
     * 추천 전체 새로고침 (3개 모두 거절 시)
     *
     * @param memberId 회원 식별자
     * @param request  새로고침 요청 (거절 ID 목록 + 위치)
     * @return 새로운 추천 3개
     */
    TodayRecommendationsResponse refreshRecommendations(String memberId,
                                                         RefreshRecommendationsRequest request);
}
