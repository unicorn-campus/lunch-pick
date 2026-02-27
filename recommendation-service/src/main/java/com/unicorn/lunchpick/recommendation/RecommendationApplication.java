package com.unicorn.lunchpick.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * 추천·이력 서비스 애플리케이션 진입점
 *
 * <p>오늘의 추천 조회, 식사 기록, 피드백, 이력 타임라인, 취향 인사이트를 제공합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.unicorn.lunchpick.recommendation", "com.unicorn.lunchpick.common"})
@EntityScan(basePackages = {"com.unicorn.lunchpick.recommendation.repository.entity", "com.unicorn.lunchpick.common.entity"})
@EnableJpaRepositories(basePackages = "com.unicorn.lunchpick.recommendation.repository.jpa")
public class RecommendationApplication {

    /**
     * 애플리케이션 시작
     *
     * @param args 커맨드라인 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
    }
}
