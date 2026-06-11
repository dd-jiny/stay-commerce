package com.staycommerce.domain.property;

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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PropertyServiceIntegrationTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("숙소를 등록할 때,")
    @Nested
    class Register {

        @DisplayName("유효한 정보를 주면, 숙소가 저장되고 id로 조회된다.")
        @Test
        void savesProperty_whenValidInfoIsProvided() {
            // act
            PropertyInfo registered = propertyService.register(
                "스테이 호텔", "서울", "서울특별시 중구 세종대로 110", "도심 속 휴식", PropertyType.HOTEL);

            // assert
            PropertyInfo found = propertyService.getById(registered.id());
            assertAll(
                () -> assertThat(found.id()).isEqualTo(registered.id()),
                () -> assertThat(found.name()).isEqualTo("스테이 호텔"),
                () -> assertThat(found.city()).isEqualTo("서울"),
                () -> assertThat(found.address()).isEqualTo("서울특별시 중구 세종대로 110"),
                () -> assertThat(found.description()).isEqualTo("도심 속 휴식"),
                () -> assertThat(found.type()).isEqualTo(PropertyType.HOTEL)
            );
        }
    }

    @DisplayName("숙소를 조회할 때,")
    @Nested
    class GetById {

        @DisplayName("존재하지 않는 id를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenIdNotFound() {
            CoreException exception = assertThrows(CoreException.class, () ->
                propertyService.getById(999_999L));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("도시로 숙소를 조회할 때,")
    @Nested
    class FindByCity {

        @DisplayName("같은 도시의 숙소만 반환한다.")
        @Test
        void returnsOnlyPropertiesInGivenCity() {
            // arrange
            propertyService.register("서울호텔A", "서울", "서울 주소 A", null, PropertyType.HOTEL);
            propertyService.register("서울호텔B", "서울", "서울 주소 B", null, PropertyType.PENSION);
            propertyService.register("부산리조트", "부산", "부산 주소", null, PropertyType.RESORT);

            // act
            List<PropertyInfo> seoul = propertyService.findByCity("서울");

            // assert
            assertAll(
                () -> assertThat(seoul).hasSize(2),
                () -> assertThat(seoul).extracting(PropertyInfo::city).containsOnly("서울"),
                () -> assertThat(seoul).extracting(PropertyInfo::name)
                    .containsExactlyInAnyOrder("서울호텔A", "서울호텔B")
            );
        }

        @DisplayName("해당 도시에 숙소가 없으면, 빈 목록을 반환한다.")
        @Test
        void returnsEmptyList_whenNoPropertyInCity() {
            // arrange
            propertyService.register("서울호텔", "서울", "서울 주소", null, PropertyType.HOTEL);

            // act
            List<PropertyInfo> result = propertyService.findByCity("제주");

            // assert
            assertThat(result).isEmpty();
        }
    }
}
