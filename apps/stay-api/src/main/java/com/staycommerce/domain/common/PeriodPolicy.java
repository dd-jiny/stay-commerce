package com.staycommerce.domain.common;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * 숙박 기간(체크인~체크아웃) 공통 유효성 규칙.
 * 체크인 inclusive / 체크아웃 exclusive. 예약 생성(T5)과 숙소 검색(T7)이 동일 규칙을 공유한다.
 */
public final class PeriodPolicy {

    private static final int MAX_NIGHTS = 30;
    private static final int MAX_ADVANCE_DAYS = 365;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private PeriodPolicy() {
    }

    /**
     * 기간 규칙 검증. 위반 시 BAD_REQUEST.
     * - 체크인/체크아웃 필수
     * - 체크아웃 > 체크인
     * - 체크인 >= 오늘(KST)
     * - 숙박 일수 <= 30박
     * - 체크인 <= 오늘 + 1년
     */
    public static void validate(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "체크인/체크아웃 날짜는 필수입니다.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "체크아웃은 체크인 이후여야 합니다.");
        }
        LocalDate today = LocalDate.now(KST);
        if (checkIn.isBefore(today)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "체크인은 오늘 이후여야 합니다.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > MAX_NIGHTS) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최대 숙박 일수(30박)를 초과했습니다.");
        }
        if (checkIn.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new CoreException(ErrorType.BAD_REQUEST, "예약 가능 기간(1년)을 초과했습니다.");
        }
    }
}
