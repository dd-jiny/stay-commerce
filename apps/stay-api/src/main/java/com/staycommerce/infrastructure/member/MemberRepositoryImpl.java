package com.staycommerce.infrastructure.member;

import com.staycommerce.domain.member.Member;
import com.staycommerce.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class MemberRepositoryImpl implements MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    @Override
    public Optional<Member> findByLoginId(String loginId) {
        return memberJpaRepository.findByLoginId(loginId);
    }

    @Override
    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }
}
