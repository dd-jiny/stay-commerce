package com.staycommerce.domain.member;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemberTest {

    private static final String ENCODED_PW = "$2a$10$dummyEncodedPasswordHash";
    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 12, 31);

    private Member createValidMember() {
        return new Member("user01", ENCODED_PW, "홍길동", BIRTHDAY, "hong@example.com", "010-1234-5678");
    }

    @DisplayName("회원 엔티티를 생성할 때,")
    @Nested
    class Create {

        @DisplayName("모든 필드가 유효하면, 회원이 생성된다.")
        @Test
        void createsSuccessfully_whenAllFieldsAreValid() {
            // act
            Member member = createValidMember();

            // assert
            assertAll(
                () -> assertThat(member.getLoginId()).isEqualTo("user01"),
                () -> assertThat(member.getPassword()).isEqualTo(ENCODED_PW),
                () -> assertThat(member.getName()).isEqualTo("홍길동"),
                () -> assertThat(member.getBirthday()).isEqualTo(BIRTHDAY),
                () -> assertThat(member.getEmail()).isEqualTo("hong@example.com"),
                () -> assertThat(member.getPhoneNumber()).isEqualTo("010-1234-5678")
            );
        }

        @DisplayName("로그인 ID가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenLoginIdIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("", ENCODED_PW, "홍길동", BIRTHDAY, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("로그인 ID에 한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenLoginIdContainsKorean() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user한글", ENCODED_PW, "홍길동", BIRTHDAY, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenLoginIdContainsSpecialChars() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user@01", ENCODED_PW, "홍길동", BIRTHDAY, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameIsBlank() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "", BIRTHDAY, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이름에 숫자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNameContainsNumbers() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "홍길3", BIRTHDAY, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenBirthdayIsNull() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "홍길동", null, "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 미래 날짜이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenBirthdayIsFuture() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "홍길동", LocalDate.now().plusDays(1), "hong@example.com", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenEmailFormatIsInvalid() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "홍길동", BIRTHDAY, "invalid-email", "010-1234-5678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("휴대폰 번호 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenPhoneNumberFormatIsInvalid() {
            CoreException exception = assertThrows(CoreException.class, () ->
                new Member("user01", ENCODED_PW, "홍길동", BIRTHDAY, "hong@example.com", "01012345678"));

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("비밀번호를 변경할 때,")
    @Nested
    class ChangePassword {

        @DisplayName("유효한 암호화된 비밀번호를 주면, 비밀번호가 변경된다.")
        @Test
        void changesPassword_whenValidEncodedPasswordIsProvided() {
            // arrange
            Member member = createValidMember();
            String newEncodedPw = "$2a$10$newEncodedPasswordHashValue";

            // act
            member.changePassword(newEncodedPw);

            // assert
            assertThat(member.getPassword()).isEqualTo(newEncodedPw);
        }

        @DisplayName("비밀번호가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNewPasswordIsBlank() {
            // arrange
            Member member = createValidMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                member.changePassword(""));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
