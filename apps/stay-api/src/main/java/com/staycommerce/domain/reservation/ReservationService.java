package com.staycommerce.domain.reservation;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ReservationService {

    private final ReservationRepository reservationRepository;

    @Transactional
    public Reservation createPending(Long memberId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                     int guests, int totalAmount, ZonedDateTime expiresAt) {
        return reservationRepository.save(
            new Reservation(memberId, roomTypeId, checkIn, checkOut, guests, totalAmount, expiresAt));
    }

    @Transactional(readOnly = true)
    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 예약입니다."));
    }

    /** 소유자 검증 포함 조회 (인가). 미존재/타인 예약 → NOT_FOUND. */
    @Transactional(readOnly = true)
    public Reservation findOwned(Long id, Long memberId) {
        return reservationRepository.findByIdAndMemberId(id, memberId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 예약입니다."));
    }

    @Transactional(readOnly = true)
    public List<ReservationInfo> findMyReservations(Long memberId) {
        return reservationRepository.findByMemberId(memberId).stream()
            .map(ReservationInfo::from)
            .toList();
    }
}
