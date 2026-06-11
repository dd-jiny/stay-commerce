package com.staycommerce.domain.inventory;

import com.staycommerce.infrastructure.inventory.DailyRoomInventoryJpaRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DailyRoomInventoryServiceIntegrationTest {

    @Autowired
    private DailyRoomInventoryService inventoryService;

    @Autowired
    private DailyRoomInventoryJpaRepository jpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private static final Long ROOM_TYPE_ID = 1L;
    private static final LocalDate D1 = LocalDate.of(2026, 7, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 7, 2);
    private static final LocalDate D3 = LocalDate.of(2026, 7, 3);

    private void saveInventory(LocalDate date, int stock) {
        jpaRepository.save(new DailyRoomInventory(ROOM_TYPE_ID, date, stock));
    }

    private DailyRoomInventory reload(LocalDate date) {
        return jpaRepository.findByRoomTypeIdAndDateIn(ROOM_TYPE_ID, List.of(date)).get(0);
    }

    @DisplayName("다일자 홀딩을 할 때,")
    @Nested
    class Hold {

        @DisplayName("모든 일자에 재고가 있으면, 각 일자의 held가 1 증가한다.")
        @Test
        void holdsAllDates_whenAllAvailable() {
            // arrange
            saveInventory(D1, 2);
            saveInventory(D2, 2);
            saveInventory(D3, 2);

            // act
            inventoryService.hold(ROOM_TYPE_ID, List.of(D1, D2, D3));

            // assert
            assertThat(reload(D1).getHeld()).isEqualTo(1);
            assertThat(reload(D2).getHeld()).isEqualTo(1);
            assertThat(reload(D3).getHeld()).isEqualTo(1);
        }

        @DisplayName("가운데 날짜의 재고가 부족하면, CONFLICT가 발생하고 앞 날짜의 홀딩도 롤백된다.")
        @Test
        void throwsAndRollsBack_whenAnyDateUnavailable() {
            // arrange: D2 재고 0
            saveInventory(D1, 2);
            saveInventory(D2, 0);
            saveInventory(D3, 2);

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                inventoryService.hold(ROOM_TYPE_ID, List.of(D1, D2, D3)));

            // assert: 예외 + 앞 날짜(D1) held 자동 복원
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
            assertThat(reload(D1).getHeld()).isZero();
            assertThat(reload(D3).getHeld()).isZero();
        }

        @DisplayName("날짜를 역순으로 입력해도, 내부에서 정렬되어 정상 처리된다.")
        @Test
        void handlesUnsortedDates() {
            // arrange
            saveInventory(D1, 1);
            saveInventory(D2, 1);
            saveInventory(D3, 1);

            // act: 역순 입력
            inventoryService.hold(ROOM_TYPE_ID, List.of(D3, D1, D2));

            // assert
            assertThat(reload(D1).getHeld()).isEqualTo(1);
            assertThat(reload(D2).getHeld()).isEqualTo(1);
            assertThat(reload(D3).getHeld()).isEqualTo(1);
        }
    }

    @DisplayName("홀딩 라이프사이클 (hold → convert → restore) 을 거칠 때,")
    @Nested
    class Lifecycle {

        @DisplayName("확정하면 held가 빠지고 stock이 차감되며, 복원하면 stock이 회복된다.")
        @Test
        void holdConvertRestore() {
            // arrange
            saveInventory(D1, 1);

            // hold
            inventoryService.hold(ROOM_TYPE_ID, List.of(D1));
            assertThat(reload(D1).getHeld()).isEqualTo(1);

            // convert (held -1, stock -1)
            inventoryService.convertHoldToStock(ROOM_TYPE_ID, List.of(D1));
            DailyRoomInventory afterConvert = reload(D1);
            assertThat(afterConvert.getHeld()).isZero();
            assertThat(afterConvert.getStock()).isZero();

            // restore (stock +1)
            inventoryService.restoreStock(ROOM_TYPE_ID, List.of(D1));
            assertThat(reload(D1).getStock()).isEqualTo(1);
        }

        @DisplayName("홀딩을 해제하면, held가 원복된다.")
        @Test
        void releaseHold() {
            saveInventory(D1, 2);
            inventoryService.hold(ROOM_TYPE_ID, List.of(D1));

            inventoryService.releaseHold(ROOM_TYPE_ID, List.of(D1));

            assertThat(reload(D1).getHeld()).isZero();
        }
    }

    @DisplayName("가용성을 확인할 때,")
    @Nested
    class IsAllAvailable {

        @DisplayName("모든 일자가 가용이면, true를 반환한다.")
        @Test
        void returnsTrue_whenAllAvailable() {
            saveInventory(D1, 1);
            saveInventory(D2, 1);

            assertThat(inventoryService.isAllAvailable(ROOM_TYPE_ID, List.of(D1, D2))).isTrue();
        }

        @DisplayName("한 일자라도 가용이 0이면, false를 반환한다.")
        @Test
        void returnsFalse_whenAnyDateFull() {
            saveInventory(D1, 1);
            saveInventory(D2, 0);

            assertThat(inventoryService.isAllAvailable(ROOM_TYPE_ID, List.of(D1, D2))).isFalse();
        }

        @DisplayName("일자가 누락되면, false를 반환한다.")
        @Test
        void returnsFalse_whenDateMissing() {
            saveInventory(D1, 1);
            // D2 행 없음

            assertThat(inventoryService.isAllAvailable(ROOM_TYPE_ID, List.of(D1, D2))).isFalse();
        }
    }
}
