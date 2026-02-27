package com.unicorn.lunchpick.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unicorn.lunchpick.member.dto.request.CardSwipeResult;
import com.unicorn.lunchpick.member.service.impl.TasteVectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * TasteVectorService 단위 테스트
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@DisplayName("TasteVectorService 단위 테스트")
class TasteVectorServiceTest {

    private TasteVectorService tasteVectorService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        tasteVectorService = new TasteVectorServiceImpl(objectMapper);
    }

    @Test
    @DisplayName("좋아요 스와이프만 있을 때 카테고리별 1.0 점수를 반환한다")
    void calculateTasteVector_allLiked_returnsOneForEachCategory() {
        // given
        List<CardSwipeResult> swipeResults = List.of(
                new CardSwipeResult("card1", true, "한식"),
                new CardSwipeResult("card2", true, "일식"),
                new CardSwipeResult("card3", true, "한식")
        );

        // when
        Map<String, Double> result = tasteVectorService.calculateTasteVector(swipeResults);

        // then
        assertThat(result).containsKey("한식");
        assertThat(result).containsKey("일식");
        assertThat(result.get("한식")).isEqualTo(1.0, within(0.01));
        assertThat(result.get("일식")).isCloseTo(0.5, within(0.01));
    }

    @Test
    @DisplayName("싫어요만 있을 때 모든 점수가 0.0이다")
    void calculateTasteVector_allDisliked_returnsZeroForAll() {
        // given
        List<CardSwipeResult> swipeResults = List.of(
                new CardSwipeResult("card1", false, "한식"),
                new CardSwipeResult("card2", false, "일식")
        );

        // when
        Map<String, Double> result = tasteVectorService.calculateTasteVector(swipeResults);

        // then
        assertThat(result.get("한식")).isEqualTo(0.0);
        assertThat(result.get("일식")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("카테고리가 null인 카드는 '기타'로 분류된다")
    void calculateTasteVector_nullCategory_classifiedAsOther() {
        // given
        List<CardSwipeResult> swipeResults = List.of(
                new CardSwipeResult("card1", true, null)
        );

        // when
        Map<String, Double> result = tasteVectorService.calculateTasteVector(swipeResults);

        // then
        assertThat(result).containsKey("기타");
        assertThat(result.get("기타")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("getTopCategories는 점수 내림차순으로 상위 N개를 반환한다")
    void getTopCategories_returnsTopNInDescendingOrder() {
        // given
        Map<String, Double> tasteVector = Map.of(
                "한식", 1.0,
                "일식", 0.8,
                "중식", 0.6,
                "양식", 0.4
        );

        // when
        List<String> topCategories = tasteVectorService.getTopCategories(tasteVector, 3);

        // then
        assertThat(topCategories).hasSize(3);
        assertThat(topCategories.get(0)).isEqualTo("한식");
        assertThat(topCategories.get(1)).isEqualTo("일식");
        assertThat(topCategories.get(2)).isEqualTo("중식");
    }

    @Test
    @DisplayName("serializeToJson과 deserializeFromJson은 역직렬화 왕복 정합성을 보장한다")
    void serializeAndDeserialize_roundTrip_isConsistent() {
        // given
        Map<String, Double> original = Map.of("한식", 0.9, "일식", 0.7);

        // when
        String json = tasteVectorService.serializeToJson(original);
        Map<String, Double> deserialized = tasteVectorService.deserializeFromJson(json);

        // then
        assertThat(deserialized).containsEntry("한식", 0.9);
        assertThat(deserialized).containsEntry("일식", 0.7);
    }

    @Test
    @DisplayName("deserializeFromJson에 null 입력 시 빈 맵을 반환한다")
    void deserializeFromJson_nullInput_returnsEmptyMap() {
        // when
        Map<String, Double> result = tasteVectorService.deserializeFromJson(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("점수 0 이하인 카테고리는 getTopCategories에서 제외된다")
    void getTopCategories_excludesZeroOrNegativeScores() {
        // given
        Map<String, Double> tasteVector = Map.of(
                "한식", 1.0,
                "중식", 0.0
        );

        // when
        List<String> topCategories = tasteVectorService.getTopCategories(tasteVector, 3);

        // then
        assertThat(topCategories).containsExactly("한식");
        assertThat(topCategories).doesNotContain("중식");
    }
}
