package com.unicorn.lunchpick.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 음식 카드 스와이프 결과 DTO
 *
 * <p>취향 온보딩 퀴즈에서 사용자가 스와이프한 음식 카드 결과를 나타냅니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record CardSwipeResult(

        /**
         * 음식 카드 ID
         */
        @NotBlank(message = "카드 ID는 필수입니다.")
        String cardId,

        /**
         * 좋아요(true) / 싫어요(false)
         */
        @NotNull(message = "좋아요 여부는 필수입니다.")
        Boolean liked,

        /**
         * 음식 카테고리 (한식, 중식, 일식, 양식, 분식, 아시안, 패스트푸드, 샐러드/건강식)
         */
        String category
) {
}
