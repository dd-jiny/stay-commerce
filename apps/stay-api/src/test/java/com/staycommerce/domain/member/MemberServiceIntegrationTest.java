package com.staycommerce.domain.member;

import com.staycommerce.infrastructure.member.MemberJpaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class MemberServiceIntegrationTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private static final String LOGIN_ID = "user01";
    private static final String RAW_PASSWORD = "Password1!";
    private static final String NAME = "홍길동";
    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 12, 31);
    private static final String EMAIL = "hong@example.com";
    private static final String PHONE = "010-1234-5678";

    private MemberInfo registerDefaultMember() {
        return memberService.register(LOGIN_ID, RAW_PASSWORD, NAME, BIRTHDAY, EMAIL, PHONE);
    }

    @DisplayName("회원을 등록할 때,")
    @Nested
    class Register {

        @DisplayName("유효한 정보를 주면, 회원이 저장된다.")
        @Test
        void savesMember_whenValidInfoIsProvided() {
            // act
            MemberInfo info = registerDefaultMember();

            // assert
            assertAll(
                () -> assertThat(info.loginId()).isEqualTo(LOGIN_ID),
                () -> assertThat(info.name()).isEqualTo(NAME),
                () -> assertThat(info.birthday()).isEqualTo(BIRTHDAY),
                () -> assertThat(info.email()).isEqualTo(EMAIL),
                () -> assertThat(info.phoneNumber()).isEqualTo(PHONE),
                () -> {
                    Member saved = memberJpaRepository.findByLoginId(LOGIN_ID).orElseThrow();
                    assertThat(saved.getPassword()).startsWith("$2a$");
                }
            );
        }

        @DisplayName("중복 로그인 ID를 주면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenLoginIdIsDuplicate() {
            // arrange
            registerDefaultMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.register(LOGIN_ID, "OtherPassword1!", "김철수",
                    LocalDate.of(1995, 1, 1), "kim@example.com", "010-5678-1234"));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("인증할 때,")
    @Nested
    class Authenticate {

        @DisplayName("올바른 loginId와 비밀번호를 주면, 예외가 발생하지 않는다.")
        @Test
        void doesNotThrow_whenCredentialsAreValid() {
            // arrange
            registerDefaultMember();

            // act & assert
            assertDoesNotThrow(() -> memberService.authenticate(LOGIN_ID, RAW_PASSWORD));
        }

        @DisplayName("존재하지 않는 loginId를 주면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        void throwsException_whenLoginIdNotFound() {
            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.authenticate("nonexistent", RAW_PASSWORD));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
        }

        @DisplayName("비밀번호가 틀리면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        void throwsException_whenPasswordIsWrong() {
            // arrange
            registerDefaultMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.authenticate(LOGIN_ID, "WrongPw123!"));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
        }
    }

    @DisplayName("비밀번호를 변경할 때,")
    @Nested
    class ChangePassword {

        @DisplayName("유효한 새 비밀번호를 주면, 비밀번호가 변경된다.")
        @Test
        void changesPassword_whenNewPasswordIsValid() {
            // arrange
            registerDefaultMember();

            // act
            memberService.changePassword(LOGIN_ID, RAW_PASSWORD, "NewPassword2@");

            // assert
            assertDoesNotThrow(() -> memberService.authenticate(LOGIN_ID, "NewPassword2@"));
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenCurrentPasswordIsWrong() {
            // arrange
            registerDefaultMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.changePassword(LOGIN_ID, "WrongPw123!", "NewPassword2@"));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("현재 비밀번호와 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNewPasswordIsSameAsCurrent() {
            // arrange
            registerDefaultMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.changePassword(LOGIN_ID, RAW_PASSWORD, RAW_PASSWORD));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("새 비밀번호가 규칙을 위반하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNewPasswordViolatesRules() {
            // arrange
            registerDefaultMember();

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                memberService.changePassword(LOGIN_ID, RAW_PASSWORD, "short"));

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
