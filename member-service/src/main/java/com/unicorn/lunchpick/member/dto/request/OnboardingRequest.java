package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 취향 온보딩 퀴즈 제출 요청 DTO
 *
 * <p>음식 카드 스와이프 결과(최소 7장)와 건강 정보 동의를 포함합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record OnboardingRequest(

        /**
         * 음식 카드 스와이프 결과 목록 (최소 7장)
         */
        @NotNull(message = "스와이프 결과 목록은 필수입니다.")
        @Size(min = 1, message = "스와이프 결과가 필요합니다.")
        @Valid
        List<CardSwipeResult> swipeResults,

        /**
         * 건강 관련 취향 정보 수집 동의 여부
         */
        @NotNull(message = "건강 정보 동의 여부는 필수입니다.")
        Boolean healthInfoConsentGiven
) {
}
