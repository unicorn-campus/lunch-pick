package com.unicorn.lunchpick.payment.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.payment.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.payment.dto.request.CancelSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.request.CreateSubscriptionRequest;
import com.unicorn.lunchpick.payment.dto.response.CancelSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.CreateSubscriptionResponse;
import com.unicorn.lunchpick.payment.dto.response.ExtendTrialResponse;
import com.unicorn.lunchpick.payment.dto.response.SubscriptionPlansResponse;
import com.unicorn.lunchpick.payment.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구독 컨트롤러
 *
 * <p>구독 플랜 조회, 구독 결제, 구독 해지, 7일 무료 연장 API를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "구독 API", description = "구독 플랜 조회 및 결제/해지 관리")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 구독 플랜 목록 조회
     *
     * @param principal 인증 사용자
     * @return 구독 플랜 목록 및 현재 플랜 정보
     */
    @Operation(summary = "구독 플랜 조회",
            description = "무료/프리미엄 플랜의 기능 비교표와 가격을 반환합니다. Redis 1시간 캐싱.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<SubscriptionPlansResponse>> getSubscriptionPlans(
            @AuthenticationPrincipal UserPrincipal principal) {
        SubscriptionPlansResponse response = subscriptionService.getSubscriptionPlans(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 구독 결제 처리
     *
     * @param principal 인증 사용자
     * @param request   구독 결제 요청
     * @return 구독 결제 응답 (201 Created)
     */
    @Operation(summary = "구독 결제",
            description = "선택한 플랜으로 PG 결제를 처리합니다. 이중결제 방지 — Retry 미적용.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping
    public ResponseEntity<ApiResponse<CreateSubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSubscriptionRequest request) {
        CreateSubscriptionResponse response = subscriptionService.createSubscription(
                principal.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /**
     * 구독 해지 예약
     *
     * @param principal      인증 사용자
     * @param subscriptionId 구독 ID
     * @param request        해지 요청
     * @return 해지 예약 응답
     */
    @Operation(summary = "구독 해지",
            description = "즉시 해지가 아닌 현재 기간 종료 후 무료 플랜으로 전환됩니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<ApiResponse<CancelSubscriptionResponse>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String subscriptionId,
            @Valid @RequestBody CancelSubscriptionRequest request) {
        CancelSubscriptionResponse response = subscriptionService.cancelSubscription(
                principal.getUserId(), subscriptionId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 7일 무료 연장 (해지 전 복귀 유도, 1회 한정)
     *
     * @param principal 인증 사용자
     * @return 연장 응답
     */
    @Operation(summary = "해지 전 7일 무료 연장",
            description = "회원당 1회 한정으로 구독 기간을 7일 연장합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/extend-trial")
    public ResponseEntity<ApiResponse<ExtendTrialResponse>> extendTrial(
            @AuthenticationPrincipal UserPrincipal principal) {
        ExtendTrialResponse response = subscriptionService.extendTrial(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
