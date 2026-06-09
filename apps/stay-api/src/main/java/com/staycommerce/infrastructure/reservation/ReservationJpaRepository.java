package com.staycommerce.infrastructure.reservation;

import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<Reservation, Long> {
    Optional<Reservation> findByIdAndMemberId(Long id, Long memberId);
    List<Reservation> findByMemberId(Long memberId);
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, ZonedDateTime threshold);
}
