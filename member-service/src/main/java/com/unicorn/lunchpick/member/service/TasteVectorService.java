package com.unicorn.lunchpick.member.service;

import com.unicorn.lunchpick.member.dto.request.CardSwipeResult;

import java.util.List;
import java.util.Map;

/**
 * 취향 벡터 계산 서비스 인터페이스
 *
 * <p>음식 카드 스와이프 결과를 카테고리별 선호도 벡터로 변환합니다.</p>
 *
 * <p><b>벡터 계산 방식:</b></p>
 * <ul>
 *   <li>좋아요(liked=true): 해당 카테고리 점수 +1.0</li>
 *   <li>싫어요(liked=false): 해당 카테고리 점수 -0.5</li>
 *   <li>최종 점수를 0.0~1.0 범위로 정규화</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public interface TasteVectorService {

    /**
     * 스와이프 결과로 취향 벡터 계산
     *
     * <p>카테고리별 가중 평균을 계산하여 0.0~1.0으로 정규화한 맵을 반환합니다.</p>
     *
     * @param swipeResults 스와이프 결과 목록
     * @return 카테고리명 → 선호도(0.0~1.0) 맵
     */
    Map<String, Double> calculateTasteVector(List<CardSwipeResult> swipeResults);

    /**
     * 취향 벡터에서 상위 카테고리 추출
     *
     * @param tasteVector 취향 벡터 맵
     * @param topN        추출할 상위 항목 수
     * @return 선호도 내림차순으로 정렬된 상위 카테고리 목록
     */
    List<String> getTopCategories(Map<String, Double> tasteVector, int topN);

    /**
     * 취향 벡터 맵을 JSON 문자열로 직렬화
     *
     * @param tasteVector 취향 벡터 맵
     * @return JSON 문자열 (JSONB 저장용)
     */
    String serializeToJson(Map<String, Double> tasteVector);

    /**
     * JSON 문자열을 취향 벡터 맵으로 역직렬화
     *
     * @param json JSON 문자열
     * @return 카테고리명 → 선호도 맵
     */
    Map<String, Double> deserializeFromJson(String json);
}
