package com.staycommerce.domain.member;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.regex.Pattern;

@Entity
@Table(name = "member")
public class Member extends BaseEntity {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^010-\\d{4}-\\d{4}$");

    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthday;

    @Column(nullable = false)
    private String email;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    protected Member() {}

    public Member(String loginId, String password, String name,
                  LocalDate birthday, String email, String phoneNumber) {
        validateLoginId(loginId);
        validatePassword(password);
        validateName(name);
        validateBirthday(birthday);
        validateEmail(email);
        validatePhoneNumber(phoneNumber);

        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.birthday = birthday;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.");
        }
        this.password = newEncodedPassword;
    }

    private void validateLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.");
        }
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 사용할 수 있습니다.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 한글 또는 영문만 사용할 수 있습니다.");
        }
    }

    private void validateBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래 날짜일 수 없습니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "휴대폰 번호는 비어있을 수 없습니다.");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "휴대폰 번호는 010-XXXX-XXXX 형식이어야 합니다.");
        }
    }
}
