package com.unicorn.lunchpick.member.dto.response;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 취향 프로파일 응답 DTO (내부 API)
 *
 * <p>추천·이력 서비스가 추천 생성 시 호출하는 내부 API 응답입니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Builder
public record TasteProfileResponse(

        /**
         * 회원 도메인 식별자 (UUID)
         */
        String memberId,

        /**
         * 취향 벡터 (카테고리명 → 선호도 0.0~1.0)
         */
        Map<String, Double> tasteVector,

        /**
         * 하드 필터 적용 알레르겐 목록
         */
        List<String> allergenFilter,

        /**
         * 식이 유형
         */
        String dietType,

        /**
         * 누적 피드백 수 (5건 미만 시 콜드스타트)
         */
        int feedbackCount,

        /**
         * 콜드스타트 여부 (feedbackCount < 5)
         */
        boolean isColdStart
) {
}
