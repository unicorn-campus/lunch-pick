package com.unicorn.lunchpick.recommendation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 추천 전체 새로고침 요청 DTO
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public record RefreshRecommendationsRequest(

        /** 거절된 추천 ID 목록 */
        @NotEmpty(message = "거절된 추천 ID 목록은 필수입니다.")
        List<String> rejectedIds,

        /** 현재 위치 위도 */
        @NotNull(message = "위도는 필수입니다.")
        Double latitude,

        /** 현재 위치 경도 */
        @NotNull(message = "경도는 필수입니다.")
        Double longitude
) {}
