package com.staycommerce.domain.member;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordValidatorTest {

    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 12, 31);

    @DisplayName("비밀번호를 검증할 때,")
    @Nested
    class Validate {

        @DisplayName("유효한 비밀번호면, 예외가 발생하지 않는다.")
        @Test
        void doesNotThrow_whenPasswordIsValid() {
            assertDoesNotThrow(() -> PasswordValidator.validate("Password1!", BIRTHDAY));
        }

        @DisplayName("8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordIsTooShort() {
            // arrange
            String shortPassword = "Pass1!";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                PasswordValidator.validate(shortPassword, BIRTHDAY));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("16자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordIsTooLong() {
            // arrange
            String longPassword = "Password1234567!!";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                PasswordValidator.validate(longPassword, BIRTHDAY));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordContainsKorean() {
            // arrange
            String koreanPassword = "Password한글1!";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                PasswordValidator.validate(koreanPassword, BIRTHDAY));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("공백이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordContainsSpace() {
            // arrange
            String spacePassword = "Pass word1!";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                PasswordValidator.validate(spacePassword, BIRTHDAY));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일(yyyyMMdd)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordContainsBirthday() {
            // arrange
            String birthdayPassword = "a19901231!";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                PasswordValidator.validate(birthdayPassword, BIRTHDAY));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
