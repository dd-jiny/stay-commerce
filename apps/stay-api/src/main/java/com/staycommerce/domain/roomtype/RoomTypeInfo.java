package com.staycommerce.domain.roomtype;

public record RoomTypeInfo(
    Long id,
    Long propertyId,
    String name,
    int standardOccupancy,
    int maxOccupancy,
    int extraPersonFee,
    int totalRoomCount
) {
    public static RoomTypeInfo from(RoomType roomType) {
        return new RoomTypeInfo(
            roomType.getId(),
            roomType.getPropertyId(),
            roomType.getName(),
            roomType.getStandardOccupancy(),
            roomType.getMaxOccupancy(),
            roomType.getExtraPersonFee(),
            roomType.getTotalRoomCount()
        );
    }
}
