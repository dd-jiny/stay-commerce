package com.staycommerce.application.reservation;

import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * 예약 유스케이스 응답 DTO (Application Layer 전용).
 * 레이어별 DTO 분리 규약에 따라 domain의 동명 record와 별개로 정의한다(T7 PropertySearchInfo와 일관).
 */
public record ReservationInfo(
    Long id,
    Long memberId,
    Long roomTypeId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int totalAmount,
    ReservationStatus status,
    ZonedDateTime expiresAt
) {
    public static ReservationInfo from(Reservation reservation) {
        return new ReservationInfo(
            reservation.getId(),
            reservation.getMemberId(),
            reservation.getRoomTypeId(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getTotalAmount(),
            reservation.getStatus(),
            reservation.getExpiresAt()
        );
    }
}
