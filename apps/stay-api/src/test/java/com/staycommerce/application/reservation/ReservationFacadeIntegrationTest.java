package com.staycommerce.application.reservation;

import com.staycommerce.domain.inventory.DailyRoomInventory;
import com.staycommerce.domain.inventory.DailyRoomInventoryRepository;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.reservation.Reservation;
import com.staycommerce.domain.reservation.ReservationStatus;
import com.staycommerce.domain.roomtype.RoomTypeInfo;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.infrastructure.reservation.ReservationJpaRepository;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import com.staycommerce.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ReservationFacade 통합 테스트(@SpringBootTest + Testcontainers).
 * 단위 테스트(ReservationFacadeTest)가 못 보는 두 가지를 실제 DB로 검증한다:
 * <ul>
 *   <li><b>원자성/롤백</b>: 재고 차감 + 예약 저장이 한 트랜잭션 — 한 날짜라도 실패하면 앞 날짜 held도 원복되고 예약은 미저장.</li>
 *   <li><b>동시성</b>: stock=1을 두고 동시 예약 시 정확히 1건만 성공(조건부 UPDATE의 오버셀 방지).</li>
 * </ul>
 */
@SpringBootTest
class ReservationFacadeIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate CHECK_IN = LocalDate.now(KST).plusDays(10);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2); // 2박: [+10, +11]
    private static final List<LocalDate> STAY_DATES = CHECK_IN.datesUntil(CHECK_OUT).toList();
    private static final Long MEMBER_ID = 1L;

    @Autowired
    private ReservationFacade facade;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private DailyRoomRateService rateService;

    @Autowired
    private DailyRoomInventoryRepository inventoryRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long registerRoomType(int standard, int max, int extraFee) {
        RoomTypeInfo info = roomTypeService.register(100L, "스탠다드", standard, max, extraFee, 5);
        return info.id();
    }

    private void registerRates(Long roomTypeId, int pricePerNight) {
        for (LocalDate date : STAY_DATES) {
            rateService.register(roomTypeId, date, pricePerNight);
        }
    }

    private void saveInventory(Long roomTypeId, LocalDate date, int stock) {
        inventoryRepository.save(new DailyRoomInventory(roomTypeId, date, stock));
    }

    private int heldOf(Long roomTypeId, LocalDate date) {
        return inventoryRepository.findByRoomTypeIdAndDateIn(roomTypeId, List.of(date)).get(0).getHeld();
    }

    private int stockOf(Long roomTypeId, LocalDate date) {
        return inventoryRepository.findByRoomTypeIdAndDateIn(roomTypeId, List.of(date)).get(0).getStock();
    }

    private ReservationCreateCommand command(Long memberId, Long roomTypeId, int guests) {
        return new ReservationCreateCommand(memberId, roomTypeId, CHECK_IN, CHECK_OUT, guests);
    }

    @DisplayName("예약 생성 트랜잭션은 원자적이라,")
    @Nested
    class Atomicity {

        @DisplayName("정상이면, 모든 일자의 held가 1 증가하고 PENDING 예약이 저장된다.")
        @Test
        void holdsAllDatesAndSaves_whenValid() {
            Long roomTypeId = registerRoomType(2, 4, 10_000);
            registerRates(roomTypeId, 100_000);
            STAY_DATES.forEach(date -> saveInventory(roomTypeId, date, 2));

            ReservationInfo info = facade.create(command(MEMBER_ID, roomTypeId, 3)); // 기준 2인 초과 1명

            assertThat(info.status()).isEqualTo(ReservationStatus.PENDING);
            assertThat(info.totalAmount()).isEqualTo(220_000); // 100000*2박 + 10000*2박
            STAY_DATES.forEach(date -> assertThat(heldOf(roomTypeId, date)).isEqualTo(1));
            assertThat(reservationJpaRepository.count()).isEqualTo(1);
        }

        @DisplayName("뒷날짜 재고가 부족하면, CONFLICT와 함께 앞날짜 held도 롤백되고 예약은 저장되지 않는다.")
        @Test
        void rollsBackHoldAndSkipsSave_whenAnyDateShort() {
            Long roomTypeId = registerRoomType(2, 4, 10_000);
            registerRates(roomTypeId, 100_000);
            saveInventory(roomTypeId, STAY_DATES.get(0), 1); // 첫날 가용
            saveInventory(roomTypeId, STAY_DATES.get(1), 0); // 둘째날 매진

            CoreException exception = assertThrows(CoreException.class,
                () -> facade.create(command(MEMBER_ID, roomTypeId, 2)));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(heldOf(roomTypeId, STAY_DATES.get(0))).isZero();   // 앞날짜 held 원복
            assertThat(reservationJpaRepository.count()).isZero();        // 예약 미저장
        }

        @DisplayName("요금이 누락된 일자가 있으면, CONFLICT와 함께 재고 홀딩도 롤백된다.")
        @Test
        void rollsBackHold_whenRateMissing() {
            Long roomTypeId = registerRoomType(2, 4, 10_000);
            rateService.register(roomTypeId, STAY_DATES.get(0), 100_000); // 첫날 요금만 등록
            STAY_DATES.forEach(date -> saveInventory(roomTypeId, date, 2));

            CoreException exception = assertThrows(CoreException.class,
                () -> facade.create(command(MEMBER_ID, roomTypeId, 2)));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            // 홀딩은 가격 계산보다 먼저 실행되지만, 같은 트랜잭션이 롤백되어 모든 날짜 held가 원복된다.
            STAY_DATES.forEach(date -> assertThat(heldOf(roomTypeId, date)).isZero());
            assertThat(reservationJpaRepository.count()).isZero();
        }
    }

    @DisplayName("확정/취소가 재고에 반영되어,")
    @Nested
    class Lifecycle {

        @DisplayName("확정하면 held가 빠지고 stock이 차감되며, 그 뒤 취소하면 stock이 복원된다.")
        @Test
        void confirmThenCancel_movesInventoryCorrectly() {
            Long roomTypeId = registerRoomType(2, 4, 0);
            registerRates(roomTypeId, 100_000);
            STAY_DATES.forEach(date -> saveInventory(roomTypeId, date, 1));

            ReservationInfo created = facade.create(command(MEMBER_ID, roomTypeId, 2));
            STAY_DATES.forEach(date -> assertThat(heldOf(roomTypeId, date)).isEqualTo(1));

            // 확정: held -1, stock -1
            ReservationInfo confirmed = facade.confirm(reservationIdOf(created), MEMBER_ID);
            assertThat(confirmed.status()).isEqualTo(ReservationStatus.CONFIRMED);
            STAY_DATES.forEach(date -> {
                assertThat(heldOf(roomTypeId, date)).isZero();
                assertThat(stockOf(roomTypeId, date)).isZero();
            });

            // CONFIRMED 취소: stock +1 (실재고 복원)
            facade.cancel(reservationIdOf(created), MEMBER_ID);
            assertThat(reload(reservationIdOf(created)).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            STAY_DATES.forEach(date -> assertThat(stockOf(roomTypeId, date)).isEqualTo(1));
        }

        @DisplayName("PENDING을 취소하면, held가 원복된다.")
        @Test
        void cancelPending_releasesHold() {
            Long roomTypeId = registerRoomType(2, 4, 0);
            registerRates(roomTypeId, 100_000);
            STAY_DATES.forEach(date -> saveInventory(roomTypeId, date, 1));

            ReservationInfo created = facade.create(command(MEMBER_ID, roomTypeId, 2));

            facade.cancel(reservationIdOf(created), MEMBER_ID);

            assertThat(reload(reservationIdOf(created)).getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            STAY_DATES.forEach(date -> assertThat(heldOf(roomTypeId, date)).isZero());
        }
    }

    @DisplayName("동시 예약을 받을 때,")
    @Nested
    class Concurrency {

        @DisplayName("stock=1 인 객실에 동시 예약이 몰려도, 정확히 1건만 성공한다(오버셀 방지).")
        @Test
        void onlyOneSucceeds_whenStockIsOne() throws InterruptedException {
            int threads = 10;
            Long roomTypeId = registerRoomType(2, 4, 0);
            registerRates(roomTypeId, 100_000);
            STAY_DATES.forEach(date -> saveInventory(roomTypeId, date, 1)); // 전 일자 재고 1

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger conflict = new AtomicInteger();

            for (int i = 0; i < threads; i++) {
                long memberId = i + 1;
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        facade.create(command(memberId, roomTypeId, 2)); // 멤버별 동일 객실/기간 경쟁
                        success.incrementAndGet();
                    } catch (CoreException e) {
                        if (e.getErrorType() == ErrorType.CONFLICT) {
                            conflict.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                        // 동시성 충돌이 드물게 다른 예외로 표출될 수 있어 성공 카운트만 엄격히 본다.
                    } finally {
                        done.countDown();
                    }
                });
            }

            ready.await();
            start.countDown(); // 일제히 출발
            done.await(30, TimeUnit.SECONDS);
            pool.shutdownNow();

            // 정확히 1건만 성공 — 나머지는 조건부 UPDATE(stock-held>=1)에 막혀 실패
            assertThat(success.get()).isEqualTo(1);
            assertThat(conflict.get()).isEqualTo(threads - 1);
            assertThat(reservationJpaRepository.count()).isEqualTo(1); // 성공분만 저장(나머지 롤백)
            STAY_DATES.forEach(date -> assertThat(heldOf(roomTypeId, date)).isEqualTo(1));
        }
    }

    private Long reservationIdOf(ReservationInfo info) {
        return info.id();
    }

    private Reservation reload(Long reservationId) {
        return reservationJpaRepository.findById(reservationId).orElseThrow();
    }
}
