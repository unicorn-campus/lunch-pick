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
 * WebClient 설정
 *
 * <p>member-service(내부 취향 프로파일 조회) 및 ai-pipeline-service(추천 생성) 호출에 사용됩니다.</p>
 *
 * <p><b>타임아웃 설정:</b></p>
 * <ul>
 *   <li>member-service: connectTimeout 5초, readTimeout 5초</li>
 *   <li>ai-pipeline-service: connectTimeout 5초, readTimeout 30초 (LLM 응답 지연 고려)</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
@Configuration
public class WebClientConfig {

    /** member-service 내부 URL (환경변수 MEMBER_SERVICE_URL) */
    @Value("${internal.member-service.url:http://localhost:8081}")
    private String memberServiceUrl;

    /** ai-pipeline-service 내부 URL (환경변수 AI_PIPELINE_SERVICE_URL) */
    @Value("${internal.ai-pipeline-service.url:http://localhost:8084}")
    private String aiPipelineServiceUrl;

    /** AI Pipeline HTTP connectTimeout (ms) */
    private static final int AI_CONNECT_TIMEOUT_MS = 5_000;

    /** AI Pipeline HTTP readTimeout (초) — LLM 응답 지연 30초 허용 */
    private static final int AI_READ_TIMEOUT_SEC = 30;

    /** AI Pipeline HTTP writeTimeout (초) */
    private static final int AI_WRITE_TIMEOUT_SEC = 10;

    /** member-service HTTP connectTimeout (ms) */
    private static final int MEMBER_CONNECT_TIMEOUT_MS = 5_000;

    /** member-service HTTP readTimeout (초) */
    private static final int MEMBER_READ_TIMEOUT_SEC = 5;

    /**
     * member-service 전용 WebClient 빈 등록
     *
     * <p>connectTimeout 5초, readTimeout 5초 설정.</p>
     *
     * @return member-service WebClient
     */
    @Bean("memberServiceWebClient")
    public WebClient memberServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, MEMBER_CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(MEMBER_READ_TIMEOUT_SEC))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(MEMBER_READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(MEMBER_READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(memberServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * ai-pipeline-service 전용 WebClient 빈 등록
     *
     * <p>LLM 응답 지연을 고려하여 connectTimeout 5초, readTimeout 30초 설정.</p>
     *
     * @return ai-pipeline-service WebClient
     */
    @Bean("aiPipelineWebClient")
    public WebClient aiPipelineWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, AI_CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(AI_READ_TIMEOUT_SEC))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(AI_READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(AI_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(aiPipelineServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
