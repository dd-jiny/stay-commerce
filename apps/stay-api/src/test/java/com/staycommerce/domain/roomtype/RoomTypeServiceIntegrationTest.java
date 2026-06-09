package com.staycommerce.domain.roomtype;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import com.staycommerce.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RoomTypeServiceIntegrationTest {

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("객실 타입을 등록할 때,")
    @Nested
    class Register {

        @DisplayName("유효한 정보를 주면, 객실 타입이 저장되고 숙소별로 조회된다.")
        @Test
        void savesRoomType_whenValidInfoIsProvided() {
            // act
            RoomTypeInfo registered = roomTypeService.register(1L, "디럭스 더블", 2, 4, 10000, 5);

            // assert
            List<RoomType> found = roomTypeService.findByPropertyId(1L);
            assertThat(found).hasSize(1);
            assertThat(found.get(0).getId()).isEqualTo(registered.id());
            assertThat(found.get(0).getName()).isEqualTo("디럭스 더블");
        }
    }

    @DisplayName("객실 타입을 조회할 때,")
    @Nested
    class GetById {

        @DisplayName("존재하지 않는 id를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenIdNotFound() {
            CoreException exception = assertThrows(CoreException.class, () ->
                roomTypeService.getById(999_999L));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("수용 가능한 객실 타입을 검색할 때,")
    @Nested
    class FindAccommodatableTypes {

        @DisplayName("요청 인원을 수용할 수 있는 객실 타입만 반환한다.")
        @Test
        void returnsOnlyTypesThatCanAccommodateGuests() {
            // arrange
            roomTypeService.register(1L, "스탠다드", 2, 2, 0, 5);     // max 2
            roomTypeService.register(1L, "디럭스", 2, 4, 10000, 5);   // max 4
            roomTypeService.register(2L, "패밀리", 4, 6, 20000, 3);   // max 6, 다른 숙소

            // act: 숙소 1,2 중 3명 수용 가능
            List<RoomType> result = roomTypeService.findAccommodatableTypes(List.of(1L, 2L), 3);

            // assert: 스탠다드(max 2)는 제외, 디럭스/패밀리만
            assertThat(result).extracting(RoomType::getName)
                .containsExactlyInAnyOrder("디럭스", "패밀리");
        }
    }
}
