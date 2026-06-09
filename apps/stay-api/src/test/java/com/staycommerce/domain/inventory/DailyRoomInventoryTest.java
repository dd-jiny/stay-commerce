package com.staycommerce.domain.inventory;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyRoomInventoryTest {

    private static final Long ROOM_TYPE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 1);

    private DailyRoomInventory inventory(int stock) {
        return new DailyRoomInventory(ROOM_TYPE_ID, DATE, stock);
    }

    @DisplayName("재고를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("유효한 값이면, held=0으로 생성된다.")
        @Test
        void createsSuccessfully_whenValid() {
            DailyRoomInventory inv = inventory(3);

            assertThat(inv.getStock()).isEqualTo(3);
            assertThat(inv.getHeld()).isZero();
            assertThat(inv.availableCount()).isEqualTo(3);
        }

        @DisplayName("재고가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenStockIsNegative() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new DailyRoomInventory(ROOM_TYPE_ID, DATE, -1));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("홀딩할 때,")
    @Nested
    class Hold {

        @DisplayName("재고가 1이면, held가 1 증가하고 가용이 0이 된다.")
        @Test
        void increasesHeld_whenStockAvailable() {
            DailyRoomInventory inv = inventory(1);

            inv.hold();

            assertThat(inv.getHeld()).isEqualTo(1);
            assertThat(inv.availableCount()).isZero();
        }

        @DisplayName("가용 재고가 0이면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenNoAvailable() {
            DailyRoomInventory inv = inventory(0);

            CoreException exception = assertThrows(CoreException.class, inv::hold);

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("홀딩을 해제할 때,")
    @Nested
    class ReleaseHold {

        @DisplayName("홀딩이 없으면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenNothingHeld() {
            DailyRoomInventory inv = inventory(2);

            CoreException exception = assertThrows(CoreException.class, inv::releaseHold);

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }

        @DisplayName("홀딩이 있으면, held가 1 감소한다.")
        @Test
        void decreasesHeld_whenHeldExists() {
            DailyRoomInventory inv = inventory(2);
            inv.hold();

            inv.releaseHold();

            assertThat(inv.getHeld()).isZero();
            assertThat(inv.availableCount()).isEqualTo(2);
        }
    }

    @DisplayName("확정할 때,")
    @Nested
    class ConvertHoldToStock {

        @DisplayName("홀딩을 확정하면, held와 stock이 함께 1 감소한다.")
        @Test
        void decreasesBoth_whenConfirmed() {
            DailyRoomInventory inv = inventory(1);
            inv.hold();

            inv.convertHoldToStock();

            assertThat(inv.getHeld()).isZero();
            assertThat(inv.getStock()).isZero();
        }

        @DisplayName("홀딩이 없으면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenNothingHeld() {
            DailyRoomInventory inv = inventory(1);

            CoreException exception = assertThrows(CoreException.class, inv::convertHoldToStock);

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("실재고를 복원할 때,")
    @Nested
    class RestoreStock {

        @DisplayName("확정 후 복원하면, stock이 1 증가한다.")
        @Test
        void increasesStock_afterConfirm() {
            DailyRoomInventory inv = inventory(1);
            inv.hold();
            inv.convertHoldToStock(); // stock=0, held=0

            inv.restoreStock();

            assertThat(inv.getStock()).isEqualTo(1);
        }
    }
}
