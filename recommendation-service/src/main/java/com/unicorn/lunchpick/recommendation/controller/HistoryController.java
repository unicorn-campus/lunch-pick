package com.unicorn.lunchpick.recommendation.controller;

import com.unicorn.lunchpick.common.dto.ApiResponse;
import com.unicorn.lunchpick.recommendation.config.jwt.UserPrincipal;
import com.unicorn.lunchpick.recommendation.dto.response.MealHistoryResponse;
import com.unicorn.lunchpick.recommendation.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 식사 이력 타임라인 컨트롤러
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Tag(name = "이력 API", description = "식사 이력 타임라인 조회")
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 식사 이력 타임라인 조회
     *
     * <p>FREE 회원: 최근 30일 제한. 30일 초과 시 403.</p>
     *
     * @param principal 인증 사용자
     * @param startDate 조회 시작일 (선택)
     * @param endDate   조회 종료일 (선택)
     * @return 식사 이력 타임라인 응답
     */
    @Operation(summary = "식사 이력 타임라인 조회",
            description = "달력 뷰 형태로 일별 식사 기록을 반환합니다. 무료: 최근 30일, 프리미엄: 무제한.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<MealHistoryResponse>> getMealHistoryTimeline(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        // 구독 정보는 간소화 처리 — 실제 운영 시 member-service 내부 API로 조회
        boolean isPremium = false;
        MealHistoryResponse response = historyService.getMealHistoryTimeline(
                principal.getUserId(), startDate, endDate, isPremium);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
