package com.staycommerce.domain.wishlist;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WishlistRepository {
    Wishlist save(Wishlist wishlist);

    Optional<Wishlist> findByMemberIdAndPropertyId(Long memberId, Long propertyId);

    boolean existsByMemberIdAndPropertyId(Long memberId, Long propertyId);

    void deleteByMemberIdAndPropertyId(Long memberId, Long propertyId);

    List<Wishlist> findByMemberId(Long memberId);

    long countByPropertyId(Long propertyId);

    /** propertyId별 찜 수. 조회되지 않은(찜 0건) propertyId는 Map에 포함되지 않는다. */
    Map<Long, Long> countByPropertyIdIn(List<Long> propertyIds);
}
