package com.staycommerce.infrastructure.reservation;

import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationRepository;
import com.staycommerce.domain.reservation.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ReservationRepositoryImpl implements ReservationRepository {

    private final ReservationJpaRepository reservationJpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        return reservationJpaRepository.save(reservation);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationJpaRepository.findById(id);
    }

    @Override
    public Optional<Reservation> findByIdAndMemberId(Long id, Long memberId) {
        return reservationJpaRepository.findByIdAndMemberId(id, memberId);
    }

    @Override
    public List<Reservation> findByMemberId(Long memberId) {
        return reservationJpaRepository.findByMemberId(memberId);
    }

    @Override
    public List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, ZonedDateTime threshold) {
        return reservationJpaRepository.findByStatusAndExpiresAtBefore(status, threshold);
    }
}
