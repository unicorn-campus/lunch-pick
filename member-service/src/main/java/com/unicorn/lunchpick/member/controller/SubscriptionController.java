package com.unicorn.lunchpick.member.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.member.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.member.dto.response.SubscriptionStatusResponse;
import com.unicorn.lunchpick.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구독 상태 컨트롤러
 *
 * <p>회원의 구독 플랜 및 이력 조회 제한 정보를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "구독 API", description = "구독 상태 조회")
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
public class SubscriptionController {

    private final MemberService memberService;

    /**
     * 구독 상태 조회
     *
     * <p>Redis 캐시에서 구독 정보를 조회하며, 캐시 미스 시 FREE 플랜 기본값을 반환합니다.</p>
     *
     * @param principal 인증된 사용자 정보
     * @return 구독 상태 응답 (plan, historyLimitDays, expiresAt)
     */
    @Operation(summary = "구독 상태 조회", description = "현재 구독 플랜과 이력 조회 가능 일수를 반환합니다.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getSubscriptionStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        SubscriptionStatusResponse response = memberService.getSubscriptionStatus(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
