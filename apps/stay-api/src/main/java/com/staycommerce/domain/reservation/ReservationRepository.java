package com.staycommerce.domain.reservation;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    Optional<Reservation> findByIdAndMemberId(Long id, Long memberId);
    List<Reservation> findByMemberId(Long memberId);
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, ZonedDateTime threshold);
}
