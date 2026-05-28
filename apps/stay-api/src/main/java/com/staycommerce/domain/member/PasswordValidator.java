package com.staycommerce.domain.member;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 16;
    private static final Pattern ALLOWED_CHARS = Pattern.compile("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?~`]+$");
    private static final DateTimeFormatter BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void validate(String rawPassword, LocalDate birthday) {
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.");
        }
        if (!ALLOWED_CHARS.matcher(rawPassword).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용할 수 있습니다.");
        }
        if (rawPassword.contains(birthday.format(BIRTHDAY_FORMAT))) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.");
        }
    }
}
