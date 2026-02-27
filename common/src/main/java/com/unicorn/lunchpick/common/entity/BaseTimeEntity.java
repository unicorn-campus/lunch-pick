package com.unicorn.lunchpick.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * JPA Auditing 베이스 엔티티
 *
 * <p>생성일시({@code createdAt})와 수정일시({@code updatedAt})를 자동으로 관리합니다.</p>
 *
 * <p><b>사용 방법:</b></p>
 * <pre>
 * {@literal @}Entity
 * public class MemberEntity extends BaseTimeEntity {
 *     // ...
 * }
 * </pre>
 *
 * <p><b>주의사항:</b></p>
 * <ul>
 *   <li>JPA Auditing 활성화를 위해 {@code @EnableJpaAuditing}이 설정 클래스에 선언되어야 합니다.</li>
 *   <li>{@link JpaConfig}에서 {@code @EnableJpaAuditing}을 적용합니다.</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see JpaConfig
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /**
     * 엔티티 생성 일시 (최초 저장 시 자동 설정, 이후 변경 불가)
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 엔티티 최종 수정 일시 (저장/수정 시 자동 갱신)
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
