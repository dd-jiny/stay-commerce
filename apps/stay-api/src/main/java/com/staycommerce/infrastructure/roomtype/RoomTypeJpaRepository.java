package com.staycommerce.infrastructure.roomtype;

import com.staycommerce.domain.roomtype.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTypeJpaRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByPropertyId(Long propertyId);
    List<RoomType> findByPropertyIdInAndMaxOccupancyGreaterThanEqual(List<Long> propertyIds, int guests);
}
