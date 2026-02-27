package com.unicorn.lunchpick.recommendation.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.recommendation.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.recommendation.dto.response.InsightsResponse;
import com.unicorn.lunchpick.recommendation.service.HistoryService;
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
 * 취향 인사이트 컨트롤러
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "인사이트 API", description = "취향 인사이트 리포트 조회")
@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightController {

    private final HistoryService historyService;

    /**
     * 취향 인사이트 리포트 조회
     *
     * <p>10건 미만 시 기록 독려 메시지, 30끼 달성 시 마일스톤 배지 포함.
     * 데이터 부족 시에도 200 반환.</p>
     *
     * @param principal 인증 사용자
     * @return 취향 인사이트 응답
     */
    @Operation(summary = "취향 인사이트 리포트 조회",
            description = "선호 카테고리 Top5, 주간 패턴, 만족도 변화를 반환합니다. 10건 미만 시에도 200 반환.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping
    public ResponseEntity<ApiResponse<InsightsResponse>> getTasteInsights(
            @AuthenticationPrincipal UserPrincipal principal) {
        InsightsResponse response = historyService.getTasteInsights(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
