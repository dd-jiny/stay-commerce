package com.staycommerce.application.reservation;

import com.staycommerce.domain.common.PeriodPolicy;
import com.staycommerce.domain.inventory.DailyRoomInventoryService;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationService;
import com.staycommerce.domain.reservation.ReservationStatus;
import com.staycommerce.domain.roomtype.RoomType;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 예약 유스케이스(생성/취소/확정) 조립. 이 프로젝트 조합 로직의 정점.
 *
 * <p>핵심 불변식:
 * <ul>
 *   <li>재고 차감 + 예약 저장은 하나의 {@code @Transactional} — 부분 차감 상태가 남지 않음(실패 시 롤백 복원).</li>
 *   <li>Reservation ↔ Inventory 직접 의존 금지 → Facade가 매개.</li>
 *   <li>상태별 재고 복원 분기(PENDING=held / CONFIRMED=stock)는 Facade 책임.</li>
 * </ul>
 */
@RequiredArgsConstructor
@Component
public class ReservationFacade {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // TODO(스코프 밖): 만료 TTL. Kafka 지연 메시지 딜레이와 같은 값을 공유해야 하므로
    //  소비자(streamer/batch) 도입 시 application.yml(@ConfigurationProperties)로 외부화한다.
    private static final Duration HOLD_TTL = Duration.ofMinutes(15);

    private final RoomTypeService roomTypeService;
    private final DailyRoomInventoryService inventoryService;
    private final DailyRoomRateService rateService;
    private final ReservationService reservationService;

    /**
     * 예약 생성. 인원 검증 → 재고 홀딩(date ASC) → 가격 계산 → PENDING 저장을 한 트랜잭션에서 수행한다.
     * 한 날짜라도 재고가 부족하면 CONFLICT로 전체 롤백되어 앞서 홀딩한 날짜도 자동 복원된다.
     */
    @Transactional
    public ReservationInfo create(ReservationCreateCommand cmd) {
        PeriodPolicy.validate(cmd.checkIn(), cmd.checkOut());

        RoomType roomType = roomTypeService.getById(cmd.roomTypeId());
        if (!roomType.canAccommodate(cmd.guests())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최대 수용 인원을 초과했습니다.");
        }

        List<LocalDate> dates = cmd.checkIn().datesUntil(cmd.checkOut()).toList(); // [checkIn, checkOut)
        int nights = dates.size();

        // 재고 홀딩을 먼저 — 동시성 실패를 빠르게 드러낸다(부족 시 CONFLICT → 롤백).
        inventoryService.hold(cmd.roomTypeId(), dates);

        // 생성 시점 가격 고정: 기간 기본요금 합 + (추가 인원 1박당 요금 × 박수)
        int total = rateService.sumBasePrice(cmd.roomTypeId(), dates)
            + roomType.extraFeeFor(cmd.guests()) * nights;

        ZonedDateTime expiresAt = ZonedDateTime.now(KST).plus(HOLD_TTL);
        Reservation reservation = reservationService.createPending(
            cmd.memberId(), cmd.roomTypeId(), cmd.checkIn(), cmd.checkOut(),
            cmd.guests(), total, expiresAt);

        // TODO(스코프 밖): AFTER_COMMIT 에 Kafka ReservationCreated(reservationId, expiresAt) 발행
        return ReservationInfo.from(reservation);
    }

    /**
     * 예약 취소. 상태별로 재고 복원 대상이 다르다 — PENDING은 임시점유(held), CONFIRMED는 실재고(stock).
     * 이 분기가 Facade의 핵심 책임이다.
     *
     * <p>순서: 도메인 상태를 먼저 전이({@code r.cancel()})한다. cancel()이 취소 불가 상태(CHECKED_IN/OUT/CANCELLED)를
     * CONFLICT로 막아 가드 역할을 하며(재고 손대기 전 차단), 이어지는 재고 네이티브 UPDATE가 상태 변경을 함께 flush 한다
     * (confirm/expire와 동일한 패턴 — 상태 변경 → 재고 반영). 모두 한 트랜잭션이라 재고 실패 시 전이도 롤백된다.
     */
    @Transactional
    public void cancel(Long reservationId, Long memberId) {
        Reservation r = reservationService.findOwned(reservationId, memberId); // 소유자 검증(미존재/타인 → NOT_FOUND)
        ReservationStatus previous = r.getStatus();
        r.cancel(); // 취소 불가 상태면 여기서 CONFLICT (재고 변경 전 차단)

        List<LocalDate> dates = r.stayDates();
        switch (previous) {
            case PENDING -> inventoryService.releaseHold(r.getRoomTypeId(), dates);   // held -= 1
            case CONFIRMED -> inventoryService.restoreStock(r.getRoomTypeId(), dates); // stock += 1
            default -> throw new CoreException(ErrorType.CONFLICT, "취소할 수 없는 상태입니다."); // 방어적(위 cancel()이 이미 차단)
        }
    }

    /**
     * 예약 확정(PENDING → CONFIRMED). 결정 1: {@code confirm()}을 먼저 호출해 만료/상태 가드를
     * 선검증함으로써, 만료된 예약에 대한 불필요한 재고 이전 SQL을 막는다(같은 트랜잭션이라 결과는 동일).
     */
    @Transactional
    public ReservationInfo confirm(Long reservationId, Long memberId) {
        Reservation r = reservationService.findOwned(reservationId, memberId);
        r.confirm(); // PENDING & 미만료 가드 → CONFIRMED (만료/불법 전이면 CONFLICT, 재고 SQL 미실행)
        inventoryService.convertHoldToStock(r.getRoomTypeId(), r.stayDates()); // held -= 1, stock -= 1
        return ReservationInfo.from(r);
    }

    /**
     * PENDING 자동 만료 진입점(스코프 밖: streamer/batch가 호출 예정).
     * 본 라운드에선 동기 흐름만 확보하고 도메인 멱등 메서드에 위임한다.
     */
    @Transactional
    public void expireIfStillPending(Long reservationId) {
        Reservation r = reservationService.findById(reservationId);
        if (r.expireIfPending()) { // 멱등: PENDING일 때만 만료하고 true
            inventoryService.releaseHold(r.getRoomTypeId(), r.stayDates());
        }
    }
}
