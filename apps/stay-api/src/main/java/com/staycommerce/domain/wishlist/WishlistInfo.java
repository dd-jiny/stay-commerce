package com.staycommerce.domain.wishlist;

public record WishlistInfo(Long id, Long memberId, Long propertyId) {

    public static WishlistInfo from(Wishlist wishlist) {
        return new WishlistInfo(wishlist.getId(), wishlist.getMemberId(), wishlist.getPropertyId());
    }
}
