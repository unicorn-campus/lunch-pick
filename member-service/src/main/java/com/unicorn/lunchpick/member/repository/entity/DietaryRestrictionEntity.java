package com.unicorn.lunchpick.member.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 식이 제한 JPA 엔티티
 *
 * <p>회원의 알레르기 항목과 식이 유형을 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_member.dietary_restriction</p>
 *
 * <p><b>주의사항:</b></p>
 * <ul>
 *   <li>민감 정보(건강 관련)이므로 {@code healthInfoConsentGiven}이 반드시 true여야 합니다.</li>
 *   <li>{@code allergens}, {@code customAllergens}는 JSONB 배열로 저장됩니다.</li>
 *   <li>{@code dietType}은 '일반', '채식', '비건', '할랄', '기타' 중 하나입니다.</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "dietary_restriction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DietaryRestrictionEntity {

    /**
     * 내부 PK
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 회원 식별자 (논리적 연결, FK 없음)
     */
    @Column(name = "member_id", nullable = false, unique = true, length = 36)
    private String memberId;

    /**
     * 알레르기 목록 (JSONB 배열, 시스템 제공 8대 알레르겐)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allergens", columnDefinition = "jsonb")
    private String allergens = "[]";

    /**
     * 사용자 직접 입력 알레르기 목록 (JSONB 배열)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_allergens", columnDefinition = "jsonb")
    private String customAllergens = "[]";

    /**
     * 식단 유형 (일반/채식/비건/할랄/기타)
     */
    @Column(name = "diet_type", nullable = false, length = 20)
    private String dietType = "일반";

    /**
     * 건강 관련 정보 수집 동의 여부 (민감정보 별도 동의)
     */
    @Column(name = "health_info_consent_given", nullable = false)
    private boolean healthInfoConsentGiven = false;

    /**
     * 갱신일시
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 빌더를 통한 엔티티 생성
     *
     * @param memberId               회원 식별자
     * @param allergens              알레르기 목록 JSON
     * @param customAllergens        직접 입력 알레르기 JSON
     * @param dietType               식단 유형
     * @param healthInfoConsentGiven 건강 정보 동의 여부
     */
    @Builder
    public DietaryRestrictionEntity(String memberId, String allergens, String customAllergens,
                                     String dietType, boolean healthInfoConsentGiven) {
        this.memberId = memberId;
        this.allergens = allergens != null ? allergens : "[]";
        this.customAllergens = customAllergens != null ? customAllergens : "[]";
        this.dietType = dietType != null ? dietType : "일반";
        this.healthInfoConsentGiven = healthInfoConsentGiven;
    }

    /**
     * 식이 제한 정보 업데이트
     *
     * @param allergens              알레르기 목록 JSON
     * @param customAllergens        직접 입력 알레르기 JSON
     * @param dietType               식단 유형
     * @param healthInfoConsentGiven 건강 정보 동의 여부
     */
    public void update(String allergens, String customAllergens, String dietType, boolean healthInfoConsentGiven) {
        this.allergens = allergens != null ? allergens : "[]";
        this.customAllergens = customAllergens != null ? customAllergens : "[]";
        this.dietType = dietType != null ? dietType : "일반";
        this.healthInfoConsentGiven = healthInfoConsentGiven;
    }
}
