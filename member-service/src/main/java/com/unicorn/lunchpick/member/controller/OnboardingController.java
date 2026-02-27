package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.member.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.member.dto.request.OnboardingProgressRequest;
import com.unicorn.lunchpick.member.dto.request.OnboardingRequest;
import com.unicorn.lunchpick.member.dto.response.OnboardingProgressSaveResponse;
import com.unicorn.lunchpick.member.dto.response.OnboardingResponse;
import com.unicorn.lunchpick.member.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 취향 온보딩 컨트롤러
 *
 * <p>음식 카드 스와이프 기반 취향 온보딩 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "온보딩 API", description = "취향 온보딩 퀴즈 제출 및 진행 상태 저장")
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * 취향 온보딩 퀴즈 제출 (완료)
     *
     * <p>스와이프 결과 7장 이상 + 건강 정보 동의 필수.</p>
     *
     * @param principal 인증된 사용자 정보
     * @param request   온보딩 퀴즈 제출 요청
     * @return 온보딩 완료 응답 (topCategories 포함)
     */
    @Operation(summary = "온보딩 퀴즈 제출", description = "스와이프 결과를 제출하여 취향 분석을 완료합니다. (최소 7장)",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "스와이프 수 부족(INSUFFICIENT_SWIPES) 또는 동의 미완료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> completeOnboarding(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OnboardingRequest request) {
        OnboardingResponse response = onboardingService.completeOnboarding(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 온보딩 진행 상태 임시 저장
     *
     * <p>앱 종료/재시작 시 진행 상태를 복원하기 위한 중간 저장입니다. Redis TTL 24시간.</p>
     *
     * @param principal 인증된 사용자 정보
     * @param request   온보딩 진행 상태 요청
     * @return 임시 저장 결과 응답
     */
    @Operation(summary = "온보딩 진행 상태 저장", description = "스와이프 중간 결과를 임시 저장합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공")
    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<OnboardingProgressSaveResponse>> saveOnboardingProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OnboardingProgressRequest request) {
        OnboardingProgressSaveResponse response = onboardingService.saveOnboardingProgress(
                principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
