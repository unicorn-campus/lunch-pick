package com.unicorn.lunchpick.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 결제 서비스 애플리케이션 진입점
 *
 * <p>구독 플랜 조회, 구독 결제, 구독 해지 기능을 제공합니다.
 * payment_history는 INSERT ONLY 원칙을 따르며 전자상거래법에 따라 5년간 보존합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@SpringBootApplication(scanBasePackages = {
        "com.unicorn.lunchpick.payment",
        "com.unicorn.lunchpick.common"
})
@EntityScan(basePackages = "com.unicorn.lunchpick.payment.repository.entity")
@EnableJpaRepositories(basePackages = "com.unicorn.lunchpick.payment.repository.jpa")
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
