package com.unicorn.lunchpick.recommendation.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * OpenWeatherMap API 전용 WebClient 설정
 *
 * <p>connectTimeout 2초, readTimeout 3초.</p>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-27
 */
@Configuration
public class WeatherWebClientConfig {

    @Value("${external.weather.url:https://api.openweathermap.org}")
    private String weatherBaseUrl;

    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final int READ_TIMEOUT_SEC = 3;
    private static final int WRITE_TIMEOUT_SEC = 3;

    @Bean("weatherWebClient")
    public WebClient weatherWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SEC))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(weatherBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
