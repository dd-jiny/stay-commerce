package com.staycommerce.domain.wishlist;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "wishlist",
    uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_member_property",
        columnNames = {"member_id", "property_id"}))
public class Wishlist extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    protected Wishlist() {
    }

    public Wishlist(Long memberId, Long propertyId) {
        validateMemberId(memberId);
        validatePropertyId(propertyId);

        this.memberId = memberId;
        this.propertyId = propertyId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private void validatePropertyId(Long propertyId) {
        if (propertyId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "숙소 ID는 필수입니다.");
        }
    }
}
