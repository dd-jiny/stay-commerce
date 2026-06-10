package com.staycommerce.infrastructure.wishlist;

import com.staycommerce.domain.wishlist.Wishlist;
import com.staycommerce.domain.wishlist.WishlistRepository;
import com.staycommerce.infrastructure.wishlist.WishlistJpaRepository.PropertyWishCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class WishlistRepositoryImpl implements WishlistRepository {

    private final WishlistJpaRepository wishlistJpaRepository;

    @Override
    public Wishlist save(Wishlist wishlist) {
        return wishlistJpaRepository.save(wishlist);
    }

    @Override
    public Optional<Wishlist> findByMemberIdAndPropertyId(Long memberId, Long propertyId) {
        return wishlistJpaRepository.findByMemberIdAndPropertyId(memberId, propertyId);
    }

    @Override
    public boolean existsByMemberIdAndPropertyId(Long memberId, Long propertyId) {
        return wishlistJpaRepository.existsByMemberIdAndPropertyId(memberId, propertyId);
    }

    @Override
    @Transactional
    public void deleteByMemberIdAndPropertyId(Long memberId, Long propertyId) {
        wishlistJpaRepository.deleteByMemberIdAndPropertyId(memberId, propertyId);
    }

    @Override
    public List<Wishlist> findByMemberId(Long memberId) {
        return wishlistJpaRepository.findByMemberId(memberId);
    }

    @Override
    public long countByPropertyId(Long propertyId) {
        return wishlistJpaRepository.countByPropertyId(propertyId);
    }

    @Override
    public Map<Long, Long> countByPropertyIdIn(List<Long> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Map.of();
        }
        return wishlistJpaRepository.countGroupedByPropertyId(propertyIds).stream()
            .collect(Collectors.toMap(PropertyWishCount::getPid, PropertyWishCount::getCnt));
    }
}
