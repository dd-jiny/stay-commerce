package com.staycommerce.application.reservation;

import java.time.LocalDate;

/** 예약 생성 입력. 기간 의미론: 체크인 inclusive / 체크아웃 exclusive. */
public record ReservationCreateCommand(
    Long memberId,
    Long roomTypeId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests
) {
}
