package com.staycommerce.interfaces.api.member;

import com.staycommerce.domain.member.MemberInfo;

import java.time.LocalDate;

public class MemberV1Dto {

    public record RegisterRequest(
        String loginId,
        String password,
        String name,
        LocalDate birthday,
        String email,
        String phoneNumber
    ) {}

    public record RegisterResponse(
        String loginId,
        String name,
        LocalDate birthday,
        String email,
        String phoneNumber
    ) {
        public static RegisterResponse from(MemberInfo info) {
            return new RegisterResponse(
                info.loginId(),
                info.name(),
                info.birthday(),
                info.email(),
                info.phoneNumber()
            );
        }
    }

    public record MyInfoResponse(
        String loginId,
        String name,
        LocalDate birthday,
        String email,
        String phoneNumber
    ) {
        public static MyInfoResponse from(MemberInfo info) {
            return new MyInfoResponse(
                info.loginId(),
                maskName(info.name()),
                info.birthday(),
                info.email(),
                maskPhoneNumber(info.phoneNumber())
            );
        }

        private static String maskName(String name) {
            if (name == null || name.isEmpty()) return name;
            if (name.length() == 1) return "*";
            return name.substring(0, name.length() - 1) + "*";
        }

        private static String maskPhoneNumber(String phoneNumber) {
            if (phoneNumber == null) return null;
            return phoneNumber.replaceAll("(010-)\\d{4}(-\\d{4})", "$1****$2");
        }
    }

    public record ChangePasswordRequest(
        String currentPassword,
        String newPassword
    ) {}
}
