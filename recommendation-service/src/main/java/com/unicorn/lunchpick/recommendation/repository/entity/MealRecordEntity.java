package com.unicorn.lunchpick.recommendation.repository.entity;

import com.unicorn.lunchpick.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 식사 기록 JPA 엔티티
 *
 * <p>추천 수락 또는 직접 기록을 통한 식사 이력을 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_recommendation.meal_record</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "meal_record",
        indexes = {
                @Index(name = "idx_meal_record_member_recorded", columnList = "member_id, recorded_at DESC")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealRecordEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** 도메인 식별자 (UUID) */
    @Column(name = "meal_id", nullable = false, unique = true, length = 36)
    private String mealId;

    /** 회원 식별자 (논리적 연결) */
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /** 연결된 추천 ID (직접 기록 시 null) */
    @Column(name = "recommendation_id", length = 36)
    private String recommendationId;

    /** 식당 식별자 */
    @Column(name = "restaurant_id", nullable = false, length = 36)
    private String restaurantId;

    /** 식당명 */
    @Column(name = "restaurant_name", nullable = false, length = 200)
    private String restaurantName;

    /** 메뉴명 */
    @Column(name = "menu_name", nullable = false, length = 200)
    private String menuName;

    /** 음식 카테고리 */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** 식사 일시 */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /**
     * 빌더를 통한 엔티티 생성
     */
    @Builder
    public MealRecordEntity(String mealId, String memberId, String recommendationId,
                             String restaurantId, String restaurantName, String menuName,
                             String category, LocalDateTime recordedAt) {
        this.mealId = mealId;
        this.memberId = memberId;
        this.recommendationId = recommendationId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.menuName = menuName != null ? menuName : "";
        this.category = category != null ? category : "기타";
        this.recordedAt = recordedAt;
    }

    /**
     * 식사 기록 수정
     *
     * @param restaurantId   새 식당 ID
     * @param restaurantName 새 식당명
     * @param menuName       새 메뉴명
     * @param recordedAt     수정된 식사 일시
     */
    public void update(String restaurantId, String restaurantName,
                       String menuName, LocalDateTime recordedAt) {
        if (restaurantId != null) this.restaurantId = restaurantId;
        if (restaurantName != null) this.restaurantName = restaurantName;
        if (menuName != null) this.menuName = menuName;
        if (recordedAt != null) this.recordedAt = recordedAt;
    }
}
