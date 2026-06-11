package com.staycommerce.application.property;

import com.staycommerce.domain.property.PropertyInfo;
import com.staycommerce.domain.property.PropertyType;
import com.staycommerce.domain.roomtype.RoomType;

import java.util.List;

/**
 * 숙소 상세: 숙소 메타 + 객실 타입 목록 + 찜 수. 기간/인원이 주어지면 객실별
 * 예약 가능 여부와 총요금 미리보기까지 채운다(F1 검색의 단일 숙소 버전).
 */
public record PropertyDetailInfo(
    Long propertyId,
    String name,
    String city,
    String address,
    String description,
    PropertyType type,
    long wishCount,
    List<RoomTypeDetail> roomTypes
) {
    public static PropertyDetailInfo of(PropertyInfo property, long wishCount, List<RoomTypeDetail> roomTypes) {
        return new PropertyDetailInfo(
            property.id(),
            property.name(),
            property.city(),
            property.address(),
            property.description(),
            property.type(),
            wishCount,
            roomTypes
        );
    }

    /**
     * 객실 타입 한 줄. {@code available}/{@code totalAmount}/{@code avgPerNight}은 기간·인원이
     * 주어진 조회에서만 채워지며, 기간 미지정 조회에서는 모두 {@code null}이다.
     */
    public record RoomTypeDetail(
        Long roomTypeId,
        String name,
        int standardOccupancy,
        int maxOccupancy,
        int extraPersonFee,
        Boolean available,
        Integer totalAmount,
        Integer avgPerNight
    ) {
        /** 기간 미지정: 객실 메타만. */
        public static RoomTypeDetail ofMetadata(RoomType roomType) {
            return new RoomTypeDetail(
                roomType.getId(), roomType.getName(),
                roomType.getStandardOccupancy(), roomType.getMaxOccupancy(), roomType.getExtraPersonFee(),
                null, null, null);
        }

        /** 기간 지정: 가용 여부 + (예약 가능할 때만) 총요금/평균가. */
        public static RoomTypeDetail ofPreview(RoomType roomType, boolean available,
                                               Integer totalAmount, Integer avgPerNight) {
            return new RoomTypeDetail(
                roomType.getId(), roomType.getName(),
                roomType.getStandardOccupancy(), roomType.getMaxOccupancy(), roomType.getExtraPersonFee(),
                available, totalAmount, avgPerNight);
        }
    }
}
