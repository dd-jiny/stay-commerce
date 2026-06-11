package com.staycommerce.infrastructure.wishlist;

import com.staycommerce.domain.wishlist.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistJpaRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByMemberIdAndPropertyId(Long memberId, Long propertyId);

    boolean existsByMemberIdAndPropertyId(Long memberId, Long propertyId);

    void deleteByMemberIdAndPropertyId(Long memberId, Long propertyId);

    List<Wishlist> findByMemberId(Long memberId);

    long countByPropertyId(Long propertyId);

    @Query("select w.propertyId as pid, count(w) as cnt from Wishlist w "
        + "where w.propertyId in :ids group by w.propertyId")
    List<PropertyWishCount> countGroupedByPropertyId(@Param("ids") List<Long> ids);

    interface PropertyWishCount {
        Long getPid();

        Long getCnt();
    }
}
