package com.staycommerce.interfaces.api;

import com.staycommerce.infrastructure.member.MemberJpaRepository;
import com.staycommerce.interfaces.api.member.MemberV1Dto;
import com.staycommerce.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest {

    private static final String REGISTER_URL = "/api/v1/members";
    private static final String MY_INFO_URL = "/api/v1/members/me";
    private static final String CHANGE_PASSWORD_URL = "/api/v1/members/me/password";

    private static final String LOGIN_ID = "user01";
    private static final String RAW_PASSWORD = "Password1!";
    private static final String NAME = "홍길동";
    private static final LocalDate BIRTHDAY = LocalDate.of(1990, 12, 31);
    private static final String EMAIL = "hong@example.com";
    private static final String PHONE = "010-1234-5678";

    private final TestRestTemplate testRestTemplate;
    private final MemberJpaRepository memberJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public MemberV1ApiE2ETest(TestRestTemplate testRestTemplate,
                              MemberJpaRepository memberJpaRepository,
                              DatabaseCleanUp databaseCleanUp) {
        this.testRestTemplate = testRestTemplate;
        this.memberJpaRepository = memberJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private MemberV1Dto.RegisterRequest defaultRegisterRequest() {
        return new MemberV1Dto.RegisterRequest(LOGIN_ID, RAW_PASSWORD, NAME, BIRTHDAY, EMAIL, PHONE);
    }

    private void registerDefaultMember() {
        testRestTemplate.postForEntity(REGISTER_URL, defaultRegisterRequest(), String.class);
    }

    private HttpHeaders authHeaders(String loginId, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", password);
        return headers;
    }

    @DisplayName("POST /api/v1/members")
    @Nested
    class Register {

        @DisplayName("유효한 정보로 가입하면, 200 성공 응답과 회원 정보를 반환한다.")
        @Test
        void returnsSuccess_whenValidRequest() {
            // act
            ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<MemberV1Dto.RegisterResponse>> response =
                testRestTemplate.exchange(REGISTER_URL, HttpMethod.POST,
                    new HttpEntity<>(defaultRegisterRequest()), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().loginId()).isEqualTo(LOGIN_ID),
                () -> assertThat(response.getBody().data().name()).isEqualTo(NAME),
                () -> assertThat(response.getBody().data().birthday()).isEqualTo(BIRTHDAY),
                () -> assertThat(response.getBody().data().email()).isEqualTo(EMAIL),
                () -> assertThat(response.getBody().data().phoneNumber()).isEqualTo(PHONE)
            );
        }

        @DisplayName("중복 로그인 ID로 가입하면, 409 CONFLICT 응답을 받는다.")
        @Test
        void returnsConflict_whenLoginIdIsDuplicate() {
            // arrange
            registerDefaultMember();

            // act
            ResponseEntity<String> response =
                testRestTemplate.postForEntity(REGISTER_URL, defaultRegisterRequest(), String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @DisplayName("잘못된 휴대폰 번호 형식이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void returnsBadRequest_whenPhoneNumberFormatIsInvalid() {
            // arrange
            MemberV1Dto.RegisterRequest request =
                new MemberV1Dto.RegisterRequest(LOGIN_ID, RAW_PASSWORD, NAME, BIRTHDAY, EMAIL, "01012345678");

            // act
            ResponseEntity<String> response =
                testRestTemplate.postForEntity(REGISTER_URL, request, String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("비밀번호 규칙 위반이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void returnsBadRequest_whenPasswordRuleIsViolated() {
            // arrange
            MemberV1Dto.RegisterRequest request =
                new MemberV1Dto.RegisterRequest(LOGIN_ID, "short", NAME, BIRTHDAY, EMAIL, PHONE);

            // act
            ResponseEntity<String> response =
                testRestTemplate.postForEntity(REGISTER_URL, request, String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("GET /api/v1/members/me")
    @Nested
    class GetMyInfo {

        @DisplayName("인증 성공 시, 마스킹된 이름과 휴대폰 번호를 반환한다.")
        @Test
        void returnsMaskedInfo_whenAuthenticated() {
            // arrange
            registerDefaultMember();

            // act
            ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<MemberV1Dto.MyInfoResponse>> response =
                testRestTemplate.exchange(MY_INFO_URL, HttpMethod.GET,
                    new HttpEntity<>(authHeaders(LOGIN_ID, RAW_PASSWORD)), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().loginId()).isEqualTo(LOGIN_ID),
                () -> assertThat(response.getBody().data().name()).isEqualTo("홍길*"),
                () -> assertThat(response.getBody().data().birthday()).isEqualTo(BIRTHDAY),
                () -> assertThat(response.getBody().data().email()).isEqualTo(EMAIL),
                () -> assertThat(response.getBody().data().phoneNumber()).isEqualTo("010-****-5678")
            );
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        void returnsUnauthorized_whenNoAuthHeaders() {
            // act
            ResponseEntity<String> response =
                testRestTemplate.getForEntity(MY_INFO_URL, String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @DisplayName("비밀번호가 틀리면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        void returnsUnauthorized_whenPasswordIsWrong() {
            // arrange
            registerDefaultMember();

            // act
            ResponseEntity<String> response =
                testRestTemplate.exchange(MY_INFO_URL, HttpMethod.GET,
                    new HttpEntity<>(authHeaders(LOGIN_ID, "WrongPw123!")), String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @DisplayName("PATCH /api/v1/members/me/password")
    @Nested
    class ChangePassword {

        @DisplayName("유효한 새 비밀번호로 변경하면, 200 성공 응답을 받는다.")
        @Test
        void returnsSuccess_whenValidPasswordChange() {
            // arrange
            registerDefaultMember();
            String newPassword = "NewPassword2@";

            // act
            HttpHeaders headers = authHeaders(LOGIN_ID, RAW_PASSWORD);
            headers.set("Content-Type", "application/json");
            MemberV1Dto.ChangePasswordRequest body = new MemberV1Dto.ChangePasswordRequest(RAW_PASSWORD, newPassword);
            ResponseEntity<String> response =
                testRestTemplate.exchange(CHANGE_PASSWORD_URL, HttpMethod.PATCH,
                    new HttpEntity<>(body, headers), String.class);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> {
                    // 새 비밀번호로 인증 성공 확인
                    ResponseEntity<String> verifyResponse =
                        testRestTemplate.exchange(MY_INFO_URL, HttpMethod.GET,
                            new HttpEntity<>(authHeaders(LOGIN_ID, newPassword)), String.class);
                    assertTrue(verifyResponse.getStatusCode().is2xxSuccessful());
                }
            );
        }

        @DisplayName("현재 비밀번호와 동일하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void returnsBadRequest_whenSamePassword() {
            // arrange
            registerDefaultMember();

            // act
            HttpHeaders headers = authHeaders(LOGIN_ID, RAW_PASSWORD);
            MemberV1Dto.ChangePasswordRequest body = new MemberV1Dto.ChangePasswordRequest(RAW_PASSWORD, RAW_PASSWORD);
            ResponseEntity<String> response =
                testRestTemplate.exchange(CHANGE_PASSWORD_URL, HttpMethod.PATCH,
                    new HttpEntity<>(body, headers), String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("비밀번호 규칙 위반이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void returnsBadRequest_whenPasswordRuleViolated() {
            // arrange
            registerDefaultMember();

            // act
            HttpHeaders headers = authHeaders(LOGIN_ID, RAW_PASSWORD);
            MemberV1Dto.ChangePasswordRequest body = new MemberV1Dto.ChangePasswordRequest(RAW_PASSWORD, "short");
            ResponseEntity<String> response =
                testRestTemplate.exchange(CHANGE_PASSWORD_URL, HttpMethod.PATCH,
                    new HttpEntity<>(body, headers), String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @DisplayName("인증 실패 시, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        void returnsUnauthorized_whenNotAuthenticated() {
            // act
            MemberV1Dto.ChangePasswordRequest body = new MemberV1Dto.ChangePasswordRequest(RAW_PASSWORD, "NewPassword2@");
            ResponseEntity<String> response =
                testRestTemplate.exchange(CHANGE_PASSWORD_URL, HttpMethod.PATCH,
                    new HttpEntity<>(body), String.class);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }
}
