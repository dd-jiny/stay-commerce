package com.staycommerce.domain.wishlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    /** 멱등: 이미 찜한 상태면 기존 찜을 그대로 반환하고 중복 저장하지 않는다. */
    @Transactional
    public WishlistInfo addIfAbsent(Long memberId, Long propertyId) {
        Wishlist wishlist = wishlistRepository.findByMemberIdAndPropertyId(memberId, propertyId)
            .orElseGet(() -> wishlistRepository.save(new Wishlist(memberId, propertyId)));
        return WishlistInfo.from(wishlist);
    }

    /** 멱등: 없는 찜을 해제해도 에러가 없다. memberId 조건으로 본인 자원만 삭제한다. */
    @Transactional
    public void removeOwned(Long memberId, Long propertyId) {
        wishlistRepository.deleteByMemberIdAndPropertyId(memberId, propertyId);
    }

    @Transactional(readOnly = true)
    public List<WishlistInfo> findMyWishlists(Long memberId) {
        return wishlistRepository.findByMemberId(memberId).stream()
            .map(WishlistInfo::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public long countByProperty(Long propertyId) {
        return wishlistRepository.countByPropertyId(propertyId);
    }

    /** 검색 조립용: 여러 숙소의 찜 수를 propertyId별 Map으로. 찜 0건 숙소는 Map에 없다(호출측에서 0 보정). */
    @Transactional(readOnly = true)
    public Map<Long, Long> countByProperties(List<Long> propertyIds) {
        return wishlistRepository.countByPropertyIdIn(propertyIds);
    }
}
