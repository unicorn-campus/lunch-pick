package com.unicorn.lunchpick.member.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.lunchpick.member.dto.request.CardSwipeResult;
import com.unicorn.lunchpick.member.service.TasteVectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 취향 벡터 계산 서비스 구현체
 *
 * <p>음식 카드 스와이프 결과를 카테고리별 선호도 벡터(0.0~1.0)로 변환합니다.</p>
 *
 * <p><b>계산 방식:</b></p>
 * <ul>
 *   <li>좋아요(liked=true): 해당 카테고리 누적 점수 +1.0</li>
 *   <li>싫어요(liked=false): 해당 카테고리 누적 점수 -0.5</li>
 *   <li>카테고리가 없는 카드는 "기타"로 분류</li>
 *   <li>최솟값 0.0 이상으로 클리핑 후 최댓값으로 정규화</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TasteVectorServiceImpl implements TasteVectorService {

    private static final String DEFAULT_CATEGORY = "기타";
    private static final double LIKE_SCORE = 1.0;
    private static final double DISLIKE_SCORE = -0.5;

    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>카드 카테고리별 가중 합산 후 최솟값 클리핑(0.0 이상) → 최댓값으로 정규화합니다.
     * 모든 점수가 0이면 빈 맵을 반환합니다.</p>
     */
    @Override
    public Map<String, Double> calculateTasteVector(List<CardSwipeResult> swipeResults) {
        Map<String, Double> rawScores = new HashMap<>();

        for (CardSwipeResult result : swipeResults) {
            String category = (result.category() != null && !result.category().isBlank())
                    ? result.category() : DEFAULT_CATEGORY;
            double delta = Boolean.TRUE.equals(result.liked()) ? LIKE_SCORE : DISLIKE_SCORE;
            rawScores.merge(category, delta, Double::sum);
        }

        // 0.0 이하 클리핑
        rawScores.replaceAll((cat, score) -> Math.max(0.0, score));

        double maxScore = rawScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        if (maxScore == 0.0) {
            log.debug("취향 벡터 계산 결과: 유효 점수 없음 (모두 0)");
            return rawScores;
        }

        final double normalizer = maxScore;
        rawScores.replaceAll((cat, score) -> Math.round((score / normalizer) * 100.0) / 100.0);

        log.debug("취향 벡터 계산 완료 — 카테고리 수: {}", rawScores.size());
        return rawScores;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTopCategories(Map<String, Double> tasteVector, int topN) {
        return tasteVector.entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String serializeToJson(Map<String, Double> tasteVector) {
        try {
            return objectMapper.writeValueAsString(tasteVector);
        } catch (JsonProcessingException ex) {
            log.error("취향 벡터 JSON 직렬화 실패", ex);
            return "{}";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Double> deserializeFromJson(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
        } catch (JsonProcessingException ex) {
            log.error("취향 벡터 JSON 역직렬화 실패 — json: {}", json, ex);
            return new HashMap<>();
        }
    }
}
