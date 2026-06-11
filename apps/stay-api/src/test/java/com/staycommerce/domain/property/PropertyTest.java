package com.staycommerce.domain.property;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertyTest {

    private static final String NAME = "스테이 호텔";
    private static final String CITY = "서울";
    private static final String ADDRESS = "서울특별시 중구 세종대로 110";
    private static final String DESCRIPTION = "도심 속 휴식";

    private Property createValidProperty() {
        return new Property(NAME, CITY, ADDRESS, DESCRIPTION, PropertyType.HOTEL);
    }

    @DisplayName("숙소 엔티티를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 유효하면, 숙소가 생성된다.")
        @Test
        void createsSuccessfully_whenAllFieldsAreValid() {
            // act
            Property property = createValidProperty();

            // assert
            assertAll(
                () -> assertThat(property.getName()).isEqualTo(NAME),
                () -> assertThat(property.getCity()).isEqualTo(CITY),
                () -> assertThat(property.getAddress()).isEqualTo(ADDRESS),
                () -> assertThat(property.getDescription()).isEqualTo(DESCRIPTION),
                () -> assertThat(property.getType()).isEqualTo(PropertyType.HOTEL)
            );
        }

        @DisplayName("소개(description)가 null이어도, 숙소가 생성된다.")
        @Test
        void createsSuccessfully_whenDescriptionIsNull() {
            // act & assert
            assertDoesNotThrow(() -> new Property(NAME, CITY, ADDRESS, null, PropertyType.HOTEL));
        }

        @DisplayName("숙소명이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Property("", CITY, ADDRESS, DESCRIPTION, PropertyType.HOTEL));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("숙소명이 200자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameExceedsMaxLength() {
            String tooLong = "가".repeat(201);

            CoreException exception = assertThrows(CoreException.class, () ->
                new Property(tooLong, CITY, ADDRESS, DESCRIPTION, PropertyType.HOTEL));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("도시가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCityIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Property(NAME, " ", ADDRESS, DESCRIPTION, PropertyType.HOTEL));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("주소가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenAddressIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Property(NAME, CITY, "", DESCRIPTION, PropertyType.HOTEL));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("숙소 유형이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenTypeIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Property(NAME, CITY, ADDRESS, DESCRIPTION, null));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
