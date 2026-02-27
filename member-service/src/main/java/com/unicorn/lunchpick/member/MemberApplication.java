package com.unicorn.lunchpick.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 회원 서비스 애플리케이션 진입점
 *
 * <p>런치픽 회원 서비스(member-service)의 Spring Boot 메인 클래스입니다.</p>
 *
 * <p><b>주요 책임:</b></p>
 * <ul>
 *   <li>카카오 OAuth2 소셜 로그인 및 자체 JWT 발급</li>
 *   <li>취향 온보딩 퀴즈 결과 저장 및 취향 벡터 생성</li>
 *   <li>위치 동의, 식이제한 설정, 프로필 관리</li>
 *   <li>구독 상태 조회 (Redis Streams 이벤트 소비)</li>
 *   <li>내부 API: 추천 서비스를 위한 취향 프로파일 제공</li>
 * </ul>
 *
 * <p><b>포트:</b> 8081</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.unicorn.lunchpick.member", "com.unicorn.lunchpick.common"})
@EntityScan(basePackages = {"com.unicorn.lunchpick.member.repository.entity", "com.unicorn.lunchpick.common.entity"})
@EnableJpaRepositories(basePackages = "com.unicorn.lunchpick.member.repository.jpa")
public class MemberApplication {

    /**
     * 애플리케이션 시작점
     *
     * @param args 커맨드라인 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(MemberApplication.class, args);
    }
}
