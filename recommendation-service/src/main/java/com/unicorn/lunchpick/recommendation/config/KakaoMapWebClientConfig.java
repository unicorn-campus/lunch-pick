package com.unicorn.lunchpick.recommendation.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 카카오맵 Place API 전용 WebClient 설정
 *
 * <p>connectTimeout 2초, readTimeout 3초 (Architect 스펙).</p>
 * <p>Authorization 헤더에 {@code KakaoAK {REST API 키}}를 자동 설정합니다.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Configuration
public class KakaoMapWebClientConfig {

    @Value("${external.map.url:https://dapi.kakao.com}")
    private String kakaoMapBaseUrl;

    @Value("${external.map.key:}")
    private String kakaoMapApiKey;

    private static final int KAKAO_CONNECT_TIMEOUT_MS = 2_000;
    private static final int KAKAO_READ_TIMEOUT_SEC = 3;
    private static final int KAKAO_WRITE_TIMEOUT_SEC = 3;

    @Bean("kakaoMapWebClient")
    public WebClient kakaoMapWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, KAKAO_CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(KAKAO_READ_TIMEOUT_SEC))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(KAKAO_READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(KAKAO_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(kakaoMapBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "KakaoAK " + kakaoMapApiKey)
                .build();
    }
}
