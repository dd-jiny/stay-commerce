package com.staycommerce.infrastructure.member;

import com.staycommerce.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByLoginId(String loginId);
}
