package com.staycommerce.domain.rate;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyRoomRateTest {

    private static final Long ROOM_TYPE_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 1);

    @DisplayName("일자별 요금을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 유효하면, 요금이 생성된다.")
        @Test
        void createsSuccessfully_whenAllFieldsAreValid() {
            DailyRoomRate rate = new DailyRoomRate(ROOM_TYPE_ID, DATE, 100000);

            assertThat(rate.getRoomTypeId()).isEqualTo(ROOM_TYPE_ID);
            assertThat(rate.getDate()).isEqualTo(DATE);
            assertThat(rate.getPrice()).isEqualTo(100000);
        }

        @DisplayName("요금이 0이어도, 생성된다.")
        @Test
        void createsSuccessfully_whenPriceIsZero() {
            assertDoesNotThrow(() -> new DailyRoomRate(ROOM_TYPE_ID, DATE, 0));
        }

        @DisplayName("객실 타입 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenRoomTypeIdIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new DailyRoomRate(null, DATE, 100000));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("날짜가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenDateIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new DailyRoomRate(ROOM_TYPE_ID, null, 100000));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("요금이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPriceIsNegative() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new DailyRoomRate(ROOM_TYPE_ID, DATE, -1));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
