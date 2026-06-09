package com.staycommerce.domain.rate;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DailyRoomRateServiceIntegrationTest {

    @Autowired
    private DailyRoomRateService rateService;

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

    @DisplayName("기간 기본요금을 합산할 때,")
    @Nested
    class SumBasePrice {

        @DisplayName("모든 일자의 요금이 존재하면, 합산 금액을 반환한다.")
        @Test
        void returnsSum_whenAllDatesHaveRate() {
            // arrange
            rateService.register(ROOM_TYPE_ID, D1, 100000);
            rateService.register(ROOM_TYPE_ID, D2, 120000);
            rateService.register(ROOM_TYPE_ID, D3, 110000);

            // act
            int total = rateService.sumBasePrice(ROOM_TYPE_ID, List.of(D1, D2, D3));

            // assert
            assertThat(total).isEqualTo(330000);
        }

        @DisplayName("한 일자라도 요금이 누락되면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenAnyDateHasNoRate() {
            // arrange: D2 요금 누락
            rateService.register(ROOM_TYPE_ID, D1, 100000);
            rateService.register(ROOM_TYPE_ID, D3, 110000);

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                rateService.sumBasePrice(ROOM_TYPE_ID, List.of(D1, D2, D3)));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("객실 타입별 요금을 조회할 때,")
    @Nested
    class FindRatesByRoomType {

        @DisplayName("roomTypeId별로 그룹핑된 Map을 반환한다.")
        @Test
        void returnsMapGroupedByRoomType() {
            // arrange
            rateService.register(1L, D1, 100000);
            rateService.register(1L, D2, 120000);
            rateService.register(2L, D1, 200000);

            // act
            Map<Long, List<DailyRoomRate>> result =
                rateService.findRatesByRoomType(List.of(1L, 2L), List.of(D1, D2));

            // assert
            assertThat(result.get(1L)).hasSize(2);
            assertThat(result.get(2L)).hasSize(1);
        }
    }
}
