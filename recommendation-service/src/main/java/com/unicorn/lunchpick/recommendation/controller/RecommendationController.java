package com.unicorn.lunchpick.recommendation.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.recommendation.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.recommendation.dto.request.AcceptRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RefreshRecommendationsRequest;
import com.unicorn.lunchpick.recommendation.dto.request.RejectRecommendationRequest;
import com.unicorn.lunchpick.recommendation.dto.response.AcceptRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RecommendationReasonResponse;
import com.unicorn.lunchpick.recommendation.dto.response.RejectRecommendationResponse;
import com.unicorn.lunchpick.recommendation.dto.response.TodayRecommendationsResponse;
import com.unicorn.lunchpick.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 추천 컨트롤러
 *
 * <p>오늘의 추천 조회, 추천 이유, 수락/거절/새로고침 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "추천 API", description = "오늘의 추천 조회 및 수락/거절")
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 오늘의 추천 3개 조회
     *
     * @param principal 인증 사용자
     * @param latitude  위도
     * @param longitude 경도
     * @return 오늘의 추천 목록
     */
    @Operation(summary = "오늘의 추천 3개 조회",
            description = "현재 위치 기반 개인화 추천을 반환합니다. 폴백 시에도 200 응답.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayRecommendationsResponse>> getTodayRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @NotNull @DecimalMin("33.0") @DecimalMax("38.9") Double latitude,
            @RequestParam @NotNull @DecimalMin("124.6") @DecimalMax("131.9") Double longitude) {
        TodayRecommendationsResponse response = recommendationService
                .getTodayRecommendations(principal.getUserId(), latitude, longitude);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 추천 이유 상세 조회
     *
     * @param principal        인증 사용자
     * @param recommendationId 추천 ID
     * @return 추천 이유 응답
     */
    @Operation(summary = "추천 이유 상세 확인",
            description = "자연어 추천 이유와 컨텍스트 태그를 반환합니다. 이유 생성 실패 시에도 200 반환.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/{recommendationId}/reason")
    public ResponseEntity<ApiResponse<RecommendationReasonResponse>> getRecommendationReason(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String recommendationId) {
        RecommendationReasonResponse response = recommendationService
                .getRecommendationReason(principal.getUserId(), recommendationId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 추천 수락
     *
     * @param principal        인증 사용자
     * @param recommendationId 추천 ID
     * @param request          수락 요청
     * @return 수락 응답
     */
    @Operation(summary = "추천 수락",
            description = "추천 수락 시각과 반응 시간을 기록합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<ApiResponse<AcceptRecommendationResponse>> acceptRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String recommendationId,
            @Valid @RequestBody AcceptRecommendationRequest request) {
        AcceptRecommendationResponse response = recommendationService
                .acceptRecommendation(principal.getUserId(), recommendationId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 추천 거절 및 대체 추천 반환
     *
     * @param principal        인증 사용자
     * @param recommendationId 추천 ID
     * @param request          거절 요청
     * @return 거절 응답 (대체 추천 포함 또는 없음)
     */
    @Operation(summary = "추천 거절 및 대체 추천",
            description = "거절 사유를 기록하고 대체 추천 1개를 반환합니다. 대체 없으면 안내 메시지 반환.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/{recommendationId}/reject")
    public ResponseEntity<ApiResponse<RejectRecommendationResponse>> rejectRecommendation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String recommendationId,
            @Valid @RequestBody RejectRecommendationRequest request) {
        RejectRecommendationResponse response = recommendationService
                .rejectRecommendation(principal.getUserId(), recommendationId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 추천 전체 새로고침
     *
     * @param principal 인증 사용자
     * @param request   새로고침 요청
     * @return 새로운 추천 3개
     */
    @Operation(summary = "추천 전체 새로고침",
            description = "3개 모두 거절 시 캐시를 무효화하고 새 추천 3개를 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TodayRecommendationsResponse>> refreshRecommendations(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RefreshRecommendationsRequest request) {
        TodayRecommendationsResponse response = recommendationService
                .refreshRecommendations(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
