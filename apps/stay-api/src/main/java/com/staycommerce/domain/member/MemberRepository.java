package com.staycommerce.domain.member;

import java.util.Optional;

public interface MemberRepository {
    Optional<Member> findByLoginId(String loginId);
    Member save(Member member);
}
