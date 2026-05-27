package com.staycommerce.domain.member;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberInfo register(String loginId, String rawPassword, String name,
                               LocalDate birthday, String email, String phoneNumber) {
        memberRepository.findByLoginId(loginId).ifPresent(m -> {
            throw new CoreException(ErrorType.CONFLICT, "이미 사용 중인 로그인 ID입니다.");
        });

        PasswordValidator.validate(rawPassword, birthday);

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Member member = new Member(loginId, encodedPassword, name, birthday, email, phoneNumber);
        return MemberInfo.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public MemberInfo getMyInfo(String loginId) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
        return MemberInfo.from(member);
    }

    @Transactional(readOnly = true)
    public void authenticate(String loginId, String rawPassword) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED));

        if (!passwordEncoder.matches(rawPassword, member.getPassword())) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
    }

    @Transactional
    public void changePassword(String loginId, String currentPassword, String newPassword) {
        Member member = memberRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
        }

        PasswordValidator.validate(newPassword, member.getBirthday());
        member.changePassword(passwordEncoder.encode(newPassword));
    }
}
