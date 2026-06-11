package com.staycommerce.application.property;

import com.staycommerce.domain.property.PropertyInfo;
import com.staycommerce.domain.property.PropertyType;
import com.staycommerce.domain.roomtype.RoomType;

/**
 * 검색 결과 한 줄: 숙소 + 예약 가능한 객실 타입 + 기간 총요금/1박 평균가 + 찜 수.
 */
public record PropertySearchInfo(
    Long propertyId,
    String propertyName,
    String city,
    PropertyType propertyType,
    Long roomTypeId,
    String roomTypeName,
    int totalAmount,
    int avgPerNight,
    long wishCount
) {
    public static PropertySearchInfo of(PropertyInfo property, RoomType roomType,
                                        int totalAmount, int avgPerNight, long wishCount) {
        return new PropertySearchInfo(
            property.id(),
            property.name(),
            property.city(),
            property.type(),
            roomType.getId(),
            roomType.getName(),
            totalAmount,
            avgPerNight,
            wishCount
        );
    }
}
