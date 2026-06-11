package com.staycommerce.application.reservation;

import com.staycommerce.domain.inventory.DailyRoomInventoryService;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationService;
import com.staycommerce.domain.reservation.ReservationStatus;
import com.staycommerce.domain.roomtype.RoomType;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ReservationFacade 단위 테스트. 도메인 서비스를 Fake로 대체해 조립 흐름/예외/가격 계산을 검증한다.
 * 롤백·동시성은 통합 테스트(@SpringBootTest + Testcontainers)에서 별도로 확인한다(설계서 §4).
 */
class ReservationFacadeTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Long MEMBER_ID = 1L;
    private static final Long ROOM_TYPE_ID = 10L;
    private static final LocalDate TODAY = LocalDate.now(KST);

    private FakeRoomTypeService roomTypeService;
    private FakeInventoryService inventoryService;
    private FakeRateService rateService;
    private FakeReservationService reservationService;
    private ReservationFacade facade;

    @BeforeEach
    void setUp() {
        roomTypeService = new FakeRoomTypeService();
        inventoryService = new FakeInventoryService();
        rateService = new FakeRateService();
        reservationService = new FakeReservationService();
        facade = new ReservationFacade(roomTypeService, inventoryService, rateService, reservationService);
    }

    /** 기준 2인 / 최대 4인 / 추가 1인당 10,000원 객실. */
    private RoomType roomType() {
        return new RoomType(100L, "디럭스", 2, 4, 10_000, 5);
    }

    private ReservationCreateCommand command(int guests) {
        return new ReservationCreateCommand(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12), guests); // 2박
    }

    @DisplayName("예약을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("정상 요청이면, PENDING 예약이 생성되고 총액 = 기본요금합 + 추가인원료*박수 이다.")
        @Test
        void createsPendingReservation_whenValid() {
            roomTypeService.register(ROOM_TYPE_ID, roomType());
            rateService.baseSum = 200_000; // 2박 기본요금 합

            ReservationInfo info = facade.create(command(3)); // 기준 2인 초과 1명 → 추가료 10,000*2박 = 20,000

            assertThat(info.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(info.totalAmount()).isEqualTo(220_000);
            assertThat(info.expiresAt()).isAfter(ZonedDateTime.now(KST));
            assertThat(inventoryService.calls).containsExactly("hold:2"); // 2일치 홀딩
        }

        @DisplayName("최대 수용 인원을 초과하면, BAD_REQUEST 이고 재고를 건드리지 않는다.")
        @Test
        void throwsBadRequest_whenExceedsMaxOccupancy() {
            roomTypeService.register(ROOM_TYPE_ID, roomType()); // 최대 4인

            assertThatThrownBy(() -> facade.create(command(5)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.BAD_REQUEST);

            assertThat(inventoryService.calls).isEmpty();   // 홀딩 미시도
            assertThat(reservationService.created).isNull(); // 저장 미시도
        }

        @DisplayName("한 날짜라도 재고가 부족하면, CONFLICT 이고 예약을 저장하지 않는다.")
        @Test
        void throwsConflict_whenInventoryShort() {
            roomTypeService.register(ROOM_TYPE_ID, roomType());
            inventoryService.holdFails = true; // 재고 부족 시뮬레이션 (실제로는 트랜잭션 롤백으로 앞 날짜 held 원복)

            assertThatThrownBy(() -> facade.create(command(2)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

            assertThat(reservationService.created).isNull();
        }

        @DisplayName("기간 내 요금 정보가 누락되면, CONFLICT 이고 예약을 저장하지 않는다.")
        @Test
        void throwsConflict_whenRateMissing() {
            roomTypeService.register(ROOM_TYPE_ID, roomType());
            rateService.missing = true;

            assertThatThrownBy(() -> facade.create(command(2)))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

            assertThat(reservationService.created).isNull();
        }
    }

    @DisplayName("예약을 취소할 때,")
    @Nested
    class Cancel {

        @DisplayName("PENDING 예약이면, 홀딩을 해제하고 CANCELLED 가 된다.")
        @Test
        void releasesHold_whenPending() {
            Reservation r = pendingReservation();
            reservationService.owned = r;

            facade.cancel(1L, MEMBER_ID);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(inventoryService.calls).containsExactly("releaseHold:2");
        }

        @DisplayName("CONFIRMED 예약이면, 실재고를 복원하고 CANCELLED 가 된다.")
        @Test
        void restoresStock_whenConfirmed() {
            Reservation r = pendingReservation();
            r.confirm();
            reservationService.owned = r;

            facade.cancel(1L, MEMBER_ID);

            assertThat(r.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(inventoryService.calls).containsExactly("restoreStock:2");
        }

        @DisplayName("CHECKED_IN 예약이면, CONFLICT 이다.")
        @Test
        void throwsConflict_whenCheckedIn() {
            Reservation r = checkedInReservation();
            reservationService.owned = r;

            assertThatThrownBy(() -> facade.cancel(1L, MEMBER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

            assertThat(inventoryService.calls).isEmpty();
        }

        @DisplayName("타인의 예약이면, NOT_FOUND 이다.")
        @Test
        void throwsNotFound_whenNotOwned() {
            reservationService.notFound = true;

            assertThatThrownBy(() -> facade.cancel(1L, 999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("예약을 확정할 때,")
    @Nested
    class Confirm {

        @DisplayName("PENDING & 미만료면, 재고를 실재고로 이전하고 CONFIRMED 가 된다.")
        @Test
        void convertsHoldToStock_whenPendingAndNotExpired() {
            Reservation r = pendingReservation();
            reservationService.owned = r;

            ReservationInfo info = facade.confirm(1L, MEMBER_ID);

            assertThat(info.status()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(inventoryService.calls).containsExactly("convertHoldToStock:2");
        }

        @DisplayName("만료된 예약이면, CONFLICT 이고 재고를 이전하지 않는다.")
        @Test
        void throwsConflict_whenExpired() {
            Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12),
                2, 200_000, ZonedDateTime.now(KST).minusMinutes(1)); // 만료 시각 과거
            reservationService.owned = r;

            assertThatThrownBy(() -> facade.confirm(1L, MEMBER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

            assertThat(inventoryService.calls).isEmpty(); // confirm() 선검증으로 재고 SQL 미실행
        }

        @DisplayName("이미 CONFIRMED 예약이면, CONFLICT 이다.")
        @Test
        void throwsConflict_whenAlreadyConfirmed() {
            Reservation r = pendingReservation();
            r.confirm();
            reservationService.owned = r;

            assertThatThrownBy(() -> facade.confirm(1L, MEMBER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType").isEqualTo(ErrorType.CONFLICT);

            assertThat(inventoryService.calls).isEmpty();
        }
    }

    // --- 픽스처 ---

    /** PENDING (체크인 today+10, 2박, 만료 미래). */
    private Reservation pendingReservation() {
        return new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY.plusDays(10), TODAY.plusDays(12),
            2, 200_000, ZonedDateTime.now(KST).plusMinutes(15));
    }

    /** CHECKED_IN (체크인 today 라 당일 체크인 가능). */
    private Reservation checkedInReservation() {
        Reservation r = new Reservation(MEMBER_ID, ROOM_TYPE_ID, TODAY, TODAY.plusDays(2),
            2, 200_000, ZonedDateTime.now(KST).plusMinutes(15));
        r.confirm();
        r.checkIn();
        return r;
    }

    // --- Fake 도메인 서비스 (생성자 인자 repository는 미사용 메서드 → null 주입) ---

    static class FakeRoomTypeService extends RoomTypeService {
        private final java.util.Map<Long, RoomType> store = new java.util.HashMap<>();

        FakeRoomTypeService() {
            super(null);
        }

        void register(Long id, RoomType roomType) {
            store.put(id, roomType);
        }

        @Override
        public RoomType getById(Long id) {
            RoomType roomType = store.get(id);
            if (roomType == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 객실 타입입니다.");
            }
            return roomType;
        }
    }

    static class FakeInventoryService extends DailyRoomInventoryService {
        final List<String> calls = new ArrayList<>();
        boolean holdFails = false;

        FakeInventoryService() {
            super(null);
        }

        @Override
        public void hold(Long roomTypeId, List<LocalDate> dates) {
            if (holdFails) {
                throw new CoreException(ErrorType.CONFLICT, "해당 기간의 재고가 부족합니다.");
            }
            calls.add("hold:" + dates.size());
        }

        @Override
        public void releaseHold(Long roomTypeId, List<LocalDate> dates) {
            calls.add("releaseHold:" + dates.size());
        }

        @Override
        public void convertHoldToStock(Long roomTypeId, List<LocalDate> dates) {
            calls.add("convertHoldToStock:" + dates.size());
        }

        @Override
        public void restoreStock(Long roomTypeId, List<LocalDate> dates) {
            calls.add("restoreStock:" + dates.size());
        }
    }

    static class FakeRateService extends DailyRoomRateService {
        int baseSum = 0;
        boolean missing = false;

        FakeRateService() {
            super(null);
        }

        @Override
        public int sumBasePrice(Long roomTypeId, List<LocalDate> dates) {
            if (missing) {
                throw new CoreException(ErrorType.CONFLICT, "해당 기간의 요금 정보가 일부 누락되었습니다.");
            }
            return baseSum;
        }
    }

    static class FakeReservationService extends ReservationService {
        Reservation owned;       // findOwned 가 반환할 예약
        Reservation created;     // createPending 으로 생성된 예약
        boolean notFound = false;

        FakeReservationService() {
            super(null);
        }

        @Override
        public Reservation createPending(Long memberId, Long roomTypeId, LocalDate checkIn, LocalDate checkOut,
                                         int guests, int totalAmount, ZonedDateTime expiresAt) {
            created = new Reservation(memberId, roomTypeId, checkIn, checkOut, guests, totalAmount, expiresAt);
            return created;
        }

        @Override
        public Reservation findOwned(Long id, Long memberId) {
            if (notFound || owned == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 예약입니다.");
            }
            return owned;
        }
    }
}
