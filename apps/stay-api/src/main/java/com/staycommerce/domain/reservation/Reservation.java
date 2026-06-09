package com.staycommerce.domain.reservation;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    private static final int MAX_NIGHTS = 30;
    private static final int MAX_ADVANCE_DAYS = 365;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private int guests;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    protected Reservation() {}

    /** PENDING 예약 생성. totalAmount/expiresAt 은 Facade가 계산해 주입한다. */
    public Reservation(Long memberId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut,
                       int guests, int totalAmount, ZonedDateTime expiresAt) {
        LocalDate today = LocalDate.now(KST);
        if (memberId == null || roomTypeId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원/객실 정보는 필수입니다.");
        }
        if (checkIn == null || checkOut == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "체크인/체크아웃 날짜는 필수입니다.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "체크아웃은 체크인 이후여야 합니다.");
        }
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
        if (guests < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "투숙 인원은 1명 이상이어야 합니다.");
        }
        if (totalAmount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0 이상이어야 합니다.");
        }

        this.memberId = memberId;
        this.roomTypeId = roomTypeId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.totalAmount = totalAmount;
        this.status = ReservationStatus.PENDING;
        this.expiresAt = expiresAt;
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** 점유 일자 [checkIn, checkOut) — 체크아웃 당일은 점유하지 않는다. */
    public List<LocalDate> stayDates() {
        return checkIn.datesUntil(checkOut).toList();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    /** PENDING → CONFIRMED. 만료 시각을 지났으면 확정 불가. */
    public void confirm() {
        if (status != ReservationStatus.PENDING) {
            throw new CoreException(ErrorType.CONFLICT, "확정할 수 없는 상태입니다.");
        }
        if (expiresAt != null && !expiresAt.isAfter(ZonedDateTime.now(KST))) {
            throw new CoreException(ErrorType.CONFLICT, "만료된 예약입니다.");
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /** PENDING 또는 CONFIRMED → CANCELLED. */
    public void cancel() {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new CoreException(ErrorType.CONFLICT, "취소할 수 없는 상태입니다.");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /** CONFIRMED → CHECKED_IN. 체크인 날짜가 되어야 가능. */
    public void checkIn() {
        if (status != ReservationStatus.CONFIRMED) {
            throw new CoreException(ErrorType.CONFLICT, "체크인할 수 없는 상태입니다.");
        }
        if (checkIn.isAfter(LocalDate.now(KST))) {
            throw new CoreException(ErrorType.CONFLICT, "체크인 날짜가 되지 않았습니다.");
        }
        this.status = ReservationStatus.CHECKED_IN;
    }

    /** CHECKED_IN → CHECKED_OUT. */
    public void checkOut() {
        if (status != ReservationStatus.CHECKED_IN) {
            throw new CoreException(ErrorType.CONFLICT, "체크아웃할 수 없는 상태입니다.");
        }
        this.status = ReservationStatus.CHECKED_OUT;
    }

    /** 멱등: PENDING일 때만 취소하고 true. 그 외엔 no-op하고 false. */
    public boolean expireIfPending() {
        if (status != ReservationStatus.PENDING) {
            return false;
        }
        this.status = ReservationStatus.CANCELLED;
        return true;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public int getGuests() {
        return guests;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public ZonedDateTime getExpiresAt() {
        return expiresAt;
    }
}
