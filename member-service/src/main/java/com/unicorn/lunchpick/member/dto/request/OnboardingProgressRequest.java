package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 온보딩 진행 상태 임시 저장 요청 DTO
 *
 * <p>중간 이탈 시 현재까지의 스와이프 결과를 임시 저장합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record OnboardingProgressRequest(

        /**
         * 현재까지 스와이프한 결과 목록
         */
        @NotNull(message = "스와이프 결과 목록은 필수입니다.")
        @Valid
        List<CardSwipeResult> swipeResults
) {
}
