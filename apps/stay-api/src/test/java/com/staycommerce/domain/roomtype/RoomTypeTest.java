package com.staycommerce.domain.roomtype;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomTypeTest {

    private static final Long PROPERTY_ID = 1L;

    private RoomType createRoomType(int standard, int max, int extraFee) {
        return new RoomType(PROPERTY_ID, "디럭스 더블", standard, max, extraFee, 5);
    }

    @DisplayName("객실 타입을 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 유효하면, 객실 타입이 생성된다.")
        @Test
        void createsSuccessfully_whenAllFieldsAreValid() {
            RoomType roomType = createRoomType(2, 4, 10000);

            assertThat(roomType.getPropertyId()).isEqualTo(PROPERTY_ID);
            assertThat(roomType.getStandardOccupancy()).isEqualTo(2);
            assertThat(roomType.getMaxOccupancy()).isEqualTo(4);
            assertThat(roomType.getExtraPersonFee()).isEqualTo(10000);
            assertThat(roomType.getTotalRoomCount()).isEqualTo(5);
        }

        @DisplayName("숙소 ID가 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPropertyIdIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new RoomType(null, "디럭스", 2, 4, 0, 5));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("객실 타입명이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new RoomType(PROPERTY_ID, " ", 2, 4, 0, 5));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("기준 인원이 1 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenStandardOccupancyIsLessThanOne() {
            CoreException exception = assertThrows(CoreException.class, () ->
                createRoomType(0, 4, 0));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("최대 인원이 기준 인원보다 작으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenMaxOccupancyIsLessThanStandard() {
            CoreException exception = assertThrows(CoreException.class, () ->
                createRoomType(4, 2, 0));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("추가 인원 요금이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenExtraPersonFeeIsNegative() {
            CoreException exception = assertThrows(CoreException.class, () ->
                createRoomType(2, 4, -1));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("보유 객실 수가 1 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenTotalRoomCountIsLessThanOne() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new RoomType(PROPERTY_ID, "디럭스", 2, 4, 0, 0));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("수용 가능 여부를 확인할 때,")
    @Nested
    class CanAccommodate {

        @DisplayName("요청 인원이 최대 인원과 같으면, true를 반환한다.")
        @Test
        void returnsTrue_whenGuestsEqualsMaxOccupancy() {
            RoomType roomType = createRoomType(2, 4, 10000);

            assertThat(roomType.canAccommodate(4)).isTrue();
        }

        @DisplayName("요청 인원이 최대 인원을 초과하면, false를 반환한다.")
        @Test
        void returnsFalse_whenGuestsExceedsMaxOccupancy() {
            RoomType roomType = createRoomType(2, 4, 10000);

            assertThat(roomType.canAccommodate(5)).isFalse();
        }

        @DisplayName("요청 인원이 1 미만이면, false를 반환한다.")
        @Test
        void returnsFalse_whenGuestsIsLessThanOne() {
            RoomType roomType = createRoomType(2, 4, 10000);

            assertThat(roomType.canAccommodate(0)).isFalse();
        }
    }

    @DisplayName("추가 인원 요금을 계산할 때,")
    @Nested
    class ExtraFeeFor {

        @DisplayName("요청 인원이 기준 인원 이하면, 0을 반환한다.")
        @Test
        void returnsZero_whenGuestsWithinStandard() {
            RoomType roomType = createRoomType(2, 4, 10000);

            assertThat(roomType.extraFeeFor(2)).isZero();
            assertThat(roomType.extraFeeFor(1)).isZero();
        }

        @DisplayName("요청 인원이 기준 인원을 초과하면, 초과 인원 × 추가요금을 반환한다.")
        @Test
        void returnsExtraFee_whenGuestsExceedStandard() {
            RoomType roomType = createRoomType(2, 4, 10000);

            // (4 - 2) * 10000
            assertThat(roomType.extraFeeFor(4)).isEqualTo(20000);
        }
    }
}
