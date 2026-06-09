package com.staycommerce.domain.roomtype;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository {
    RoomType save(RoomType roomType);
    Optional<RoomType> findById(Long id);
    List<RoomType> findByPropertyId(Long propertyId);
    List<RoomType> findByPropertyIdInAndMaxOccupancyGreaterThanEqual(List<Long> propertyIds, int guests);
}
