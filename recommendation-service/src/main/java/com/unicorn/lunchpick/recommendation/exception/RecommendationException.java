package com.unicorn.lunchpick.recommendation.exception;

import com.unicorn.lunchpick.common.exception.BusinessException;

/**
 * 추천·이력 서비스 전용 비즈니스 예외
 *
 * <p><b>주요 에러 코드:</b></p>
 * <ul>
 *   <li>{@code RECOMMENDATION_NOT_FOUND} — 추천 없음</li>
 *   <li>{@code MEAL_NOT_FOUND} — 식사 기록 없음</li>
 *   <li>{@code DUPLICATE_MEAL_RECORD} — 중복 식사 기록</li>
 *   <li>{@code INVALID_MEAL_TIME} — 식사 시간대 외 기록</li>
 *   <li>{@code CANCEL_TIMEOUT} — 30초 초과 취소 불가</li>
 *   <li>{@code PREMIUM_REQUIRED} — 프리미엄 회원 전용</li>
 *   <li>{@code LOCATION_REQUIRED} — 위치 정보 필요</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public class RecommendationException extends BusinessException {

    public RecommendationException(String errorCode, String message) {
        super(errorCode, message);
    }

    /** 추천 없음 */
    public static RecommendationException recommendationNotFound() {
        return new RecommendationException("RECOMMENDATION_NOT_FOUND", "해당 추천 정보를 찾을 수 없습니다.");
    }

    /** 식사 기록 없음 */
    public static RecommendationException mealNotFound() {
        return new RecommendationException("MEAL_NOT_FOUND", "해당 식사 기록을 찾을 수 없습니다.");
    }

    /** 중복 식사 기록 */
    public static RecommendationException duplicateMealRecord() {
        return new RecommendationException("DUPLICATE_MEAL_RECORD", "이미 기록되었어요. 수정하시겠어요?");
    }

    /** 식사 시간대 외 기록 */
    public static RecommendationException invalidMealTime() {
        return new RecommendationException("INVALID_MEAL_TIME", "점심 식사 기록은 10:30~15:00 사이에만 가능해요.");
    }

    /** 30초 초과 취소 불가 */
    public static RecommendationException cancelTimeout() {
        return new RecommendationException("CANCEL_TIMEOUT", "이력 화면에서 수정할 수 있어요.");
    }

    /** 프리미엄 전용 기능 */
    public static RecommendationException premiumRequired() {
        return new RecommendationException("PREMIUM_REQUIRED", "프리미엄에서 전체 이력을 확인하세요.");
    }

    /** 위치 정보 필요 */
    public static RecommendationException locationRequired() {
        return new RecommendationException("LOCATION_REQUIRED", "위치를 설정해주세요.");
    }
}
