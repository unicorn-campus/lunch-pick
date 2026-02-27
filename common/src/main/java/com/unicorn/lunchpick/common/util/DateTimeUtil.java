package com.unicorn.lunchpick.common.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 유틸리티
 *
 * <p>런치픽 서비스에서 공통으로 사용하는 날짜/시간 관련 유틸리티 메서드를 제공합니다.
 * 모든 시각은 한국 표준시(KST, Asia/Seoul)를 기준으로 합니다.</p>
 *
 * <p><b>주요 기능:</b></p>
 * <ul>
 *   <li>KST 현재 시각 조회 ({@link #nowKst()})</li>
 *   <li>점심 시간대 판별 ({@link #isLunchTime()})</li>
 *   <li>평일/주말 판별 ({@link #isWeekday()})</li>
 *   <li>ISO 8601 포맷 변환 ({@link #toIso8601(LocalDateTime)})</li>
 * </ul>
 *
 * @author lunchpick-team
 * @version 1.0
 * @since 2026-02-26
 */
public final class DateTimeUtil {

    /** 한국 표준시 ZoneId */
    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 점심 시간 시작 (11:00 KST) */
    private static final LocalTime LUNCH_START = LocalTime.of(11, 0);

    /** 점심 시간 종료 (14:00 KST) */
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

    /** ISO 8601 포맷터 */
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private DateTimeUtil() {
        // 유틸리티 클래스 — 인스턴스 생성 불가
    }

    /**
     * KST 현재 시각 조회
     *
     * @return 현재 KST 시각 ({@link LocalDateTime})
     */
    public static LocalDateTime nowKst() {
        return ZonedDateTime.now(KST).toLocalDateTime();
    }

    /**
     * 현재 시각이 점심 시간대인지 판별
     *
     * <p>점심 시간대: 11:00 ~ 14:00 KST</p>
     *
     * @return 점심 시간대이면 {@code true}
     */
    public static boolean isLunchTime() {
        LocalTime now = nowKst().toLocalTime();
        return !now.isBefore(LUNCH_START) && now.isBefore(LUNCH_END);
    }

    /**
     * 현재 시각이 점심 시간대인지 판별 (기준 시각 지정)
     *
     * @param dateTime 판별 기준 시각 (KST)
     * @return 점심 시간대이면 {@code true}
     */
    public static boolean isLunchTime(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(LUNCH_START) && time.isBefore(LUNCH_END);
    }

    /**
     * 현재 요일이 평일(월~금)인지 판별
     *
     * @return 평일이면 {@code true}, 주말이면 {@code false}
     */
    public static boolean isWeekday() {
        DayOfWeek day = nowKst().getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    /**
     * 현재 요일 반환
     *
     * @return 현재 KST 요일 ({@link DayOfWeek})
     */
    public static DayOfWeek currentDayOfWeek() {
        return nowKst().getDayOfWeek();
    }

    /**
     * LocalDateTime을 ISO 8601 문자열로 변환
     *
     * <p>형식: {@code yyyy-MM-dd'T'HH:mm:ss}</p>
     *
     * @param dateTime 변환할 시각
     * @return ISO 8601 형식 문자열
     */
    public static String toIso8601(LocalDateTime dateTime) {
        return dateTime.format(ISO_FORMATTER);
    }

    /**
     * KST 현재 시각을 ISO 8601 문자열로 반환
     *
     * @return ISO 8601 형식 현재 KST 시각 문자열
     */
    public static String nowKstIso8601() {
        return toIso8601(nowKst());
    }
}
