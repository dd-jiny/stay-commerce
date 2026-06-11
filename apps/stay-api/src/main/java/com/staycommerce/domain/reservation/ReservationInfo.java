package com.staycommerce.domain.reservation;

import java.time.LocalDate;
import java.time.ZonedDateTime;

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
