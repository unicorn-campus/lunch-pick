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
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 위치 동의 이력 JPA 엔티티
 *
 * <p>회원의 위치 정보 동의/거절 이력을 저장합니다.</p>
 *
 * <p><b>스키마:</b> lunchpick_member.location_consent</p>
 *
 * <p><b>주의사항:</b></p>
 * <ul>
 *   <li>동의 이력은 INSERT 전용으로 관리됩니다.</li>
 *   <li>위치정보법 준수: 수집 목적 고지, 보유 기간(6개월) 고지 후 동의 여부 기록</li>
 *   <li>동의 변경 시 기존 레코드를 수정하지 않고 새 레코드를 INSERT합니다.</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Entity
@Table(name = "location_consent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocationConsentEntity {

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
    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    /**
     * 동의 여부 (true: 동의, false: 거절)
     */
    @Column(name = "consented", nullable = false)
    private boolean consented;

    /**
     * 동의 일시 (클라이언트 기준 시각)
     */
    @Column(name = "consented_at")
    private LocalDateTime consentedAt;

    /**
     * 레코드 생성 일시 (서버 기준)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 빌더를 통한 위치 동의 이력 생성
     *
     * @param memberId    회원 식별자
     * @param consented   동의 여부
     * @param consentedAt 동의 일시
     */
    @Builder
    public LocationConsentEntity(String memberId, boolean consented, LocalDateTime consentedAt) {
        this.memberId = memberId;
        this.consented = consented;
        this.consentedAt = consentedAt;
    }
}
