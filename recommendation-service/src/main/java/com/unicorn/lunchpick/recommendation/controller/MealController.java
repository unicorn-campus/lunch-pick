package com.unicorn.lunchpick.recommendation.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.recommendation.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.recommendation.dto.request.CreateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.request.FeedbackRequest;
import com.unicorn.lunchpick.recommendation.dto.request.UpdateMealRequest;
import com.unicorn.lunchpick.recommendation.dto.response.FeedbackResponse;
import com.unicorn.lunchpick.recommendation.dto.response.MealResponse;
import com.unicorn.lunchpick.recommendation.service.MealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 식사 기록 컨트롤러
 *
 * <p>식사 기록 생성/수정/취소 및 피드백 제출 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "식사 기록 API", description = "식사 기록 관리 및 피드백 제출")
@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    /**
     * 식사 완료 원탭 기록 생성
     *
     * @param principal 인증 사용자
     * @param request   식사 기록 요청
     * @return 식사 기록 응답 (201 Created)
     */
    @Operation(summary = "식사 완료 기록",
            description = "식사 완료를 1탭으로 기록합니다. 10:30~15:00 사이에만 가능.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping
    public ResponseEntity<ApiResponse<MealResponse>> createMeal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMealRequest request) {
        MealResponse response = mealService.createMeal(principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * 식사 기록 수정
     *
     * @param principal 인증 사용자
     * @param mealId    식사 기록 ID
     * @param request   수정 요청
     * @return 수정된 식사 기록 응답
     */
    @Operation(summary = "식사 기록 수정",
            description = "식당, 메뉴, 기록 시간을 수정합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/{mealId}")
    public ResponseEntity<ApiResponse<MealResponse>> updateMeal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String mealId,
            @Valid @RequestBody UpdateMealRequest request) {
        MealResponse response = mealService.updateMeal(principal.getUserId(), mealId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 식사 기록 취소 (30초 이내)
     *
     * @param principal 인증 사용자
     * @param mealId    식사 기록 ID
     * @return 취소 완료 메시지
     */
    @Operation(summary = "식사 기록 취소",
            description = "기록 후 30초 이내에만 취소 가능합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/{mealId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteMeal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String mealId) {
        mealService.deleteMeal(principal.getUserId(), mealId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "기록이 취소되었어요.")));
    }

    /**
     * 식사 피드백 제출
     *
     * @param principal 인증 사용자
     * @param mealId    식사 기록 ID
     * @param request   피드백 요청
     * @return 피드백 응답
     */
    @Operation(summary = "식사 피드백 제출",
            description = "만족도(GOOD/BAD/NEUTRAL)와 키워드(TASTE/PORTION/SPEED)를 제출합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/{mealId}/feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String mealId,
            @Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = mealService.submitFeedback(principal.getUserId(), mealId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
