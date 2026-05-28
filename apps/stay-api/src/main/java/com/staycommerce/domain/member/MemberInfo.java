package com.staycommerce.domain.member;

import java.time.LocalDate;

public record MemberInfo(
    String loginId,
    String name,
    LocalDate birthday,
    String email,
    String phoneNumber
) {
    public static MemberInfo from(Member member) {
        return new MemberInfo(
            member.getLoginId(),
            member.getName(),
            member.getBirthday(),
            member.getEmail(),
            member.getPhoneNumber()
        );
    }
}
