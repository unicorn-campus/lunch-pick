package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 취향 온보딩 퀴즈 제출 응답 DTO
 *
 * <p>온보딩 완료 시 선호 카테고리 Top 3와 취향 벡터 생성 결과를 반환합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record OnboardingResponse(

        /**
         * 안내 메시지
         */
        String message,

        /**
         * 선호 카테고리 Top 3
         */
        List<String> topCategories,

        /**
         * 취향 벡터 생성 완료 여부
         */
        boolean tasteVectorCreated
) {
}
