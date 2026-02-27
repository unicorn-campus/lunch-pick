package com.unicorn.lunchpick.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 공통 설정
 *
 * <p>JPA Auditing을 활성화하여 {@code BaseTimeEntity}의
 * {@code createdAt}, {@code updatedAt} 자동 관리를 지원합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 * @see com.unicorn.lunchpick.common.entity.BaseTimeEntity
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
