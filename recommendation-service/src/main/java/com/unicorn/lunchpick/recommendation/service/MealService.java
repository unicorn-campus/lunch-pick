package com.unicorn.lunchpick.recommendation.service;

import com.unicorn.lunchpick.recommendation.dto.request.CreateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.request.FeedbackRequest;
import com.unicorn.lunchpick.recommendation.dto.request.UpdateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.response.FeedbackResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealResponse;

/**
 * 식사 기록 서비스 인터페이스
 *
 * <p>식사 기록 생성/수정/취소 및 피드백 제출을 담당합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface MealService {

    /**
     * 식사 완료 기록 생성
     *
     * <p>식사 시간대(10:30~15:00) 및 당일 중복 기록 여부를 검증합니다.</p>
     *
     * @param memberId 회원 식별자
     * @param request  식사 기록 생성 요청
     * @return 식사 기록 응답
     */
    MealResponse createMeal(String memberId, CreateMealRequest request);

    /**
     * 식사 기록 수정
     *
     * @param memberId 회원 식별자
     * @param mealId   식사 기록 ID
     * @param request  수정 요청
     * @return 수정된 식사 기록 응답
     */
    MealResponse updateMeal(String memberId, String mealId, UpdateMealRequest request);

    /**
     * 식사 기록 취소 (30초 이내만 가능)
     *
     * <p>30초 초과 시 {@code CANCEL_TIMEOUT} 예외가 발생합니다.</p>
     *
     * @param memberId 회원 식별자
     * @param mealId   식사 기록 ID
     */
    void deleteMeal(String memberId, String mealId);

    /**
     * 식사 피드백 제출
     *
     * @param memberId 회원 식별자
     * @param mealId   식사 기록 ID
     * @param request  피드백 요청
     * @return 피드백 응답
     */
    FeedbackResponse submitFeedback(String memberId, String mealId, FeedbackRequest request);
}
