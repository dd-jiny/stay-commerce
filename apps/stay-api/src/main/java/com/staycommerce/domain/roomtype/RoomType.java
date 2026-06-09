package com.staycommerce.domain.roomtype;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_type")
public class RoomType extends BaseEntity {

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "standard_occupancy", nullable = false)
    private int standardOccupancy;

    @Column(name = "max_occupancy", nullable = false)
    private int maxOccupancy;

    @Column(name = "extra_person_fee", nullable = false)
    private int extraPersonFee;

    @Column(name = "total_room_count", nullable = false)
    private int totalRoomCount;

    protected RoomType() {}

    public RoomType(Long propertyId, String name, int standardOccupancy,
                    int maxOccupancy, int extraPersonFee, int totalRoomCount) {
        if (propertyId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "숙소 ID는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "객실 타입명은 비어있을 수 없습니다.");
        }
        if (standardOccupancy < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "기준 인원은 1명 이상이어야 합니다.");
        }
        if (maxOccupancy < standardOccupancy) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최대 인원은 기준 인원 이상이어야 합니다.");
        }
        if (extraPersonFee < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "추가 인원 요금은 0 이상이어야 합니다.");
        }
        if (totalRoomCount < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "보유 객실 수는 1 이상이어야 합니다.");
        }

        this.propertyId = propertyId;
        this.name = name;
        this.standardOccupancy = standardOccupancy;
        this.maxOccupancy = maxOccupancy;
        this.extraPersonFee = extraPersonFee;
        this.totalRoomCount = totalRoomCount;
    }

    /** 요청 인원이 최대 수용 인원 이내인지. 초과 시 예약 실패의 근거. */
    public boolean canAccommodate(int guests) {
        return guests >= 1 && guests <= maxOccupancy;
    }

    /** 추가 인원 1박당 요금 = max(0, guests - standard) * extraPersonFee. (박수 곱은 호출자가 수행) */
    public int extraFeeFor(int guests) {
        return Math.max(0, guests - standardOccupancy) * extraPersonFee;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public String getName() {
        return name;
    }

    public int getStandardOccupancy() {
        return standardOccupancy;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public int getExtraPersonFee() {
        return extraPersonFee;
    }

    public int getTotalRoomCount() {
        return totalRoomCount;
    }
}
