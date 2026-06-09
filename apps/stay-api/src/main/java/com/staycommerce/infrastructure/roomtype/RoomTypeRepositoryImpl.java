package com.staycommerce.infrastructure.roomtype;

import com.staycommerce.domain.roomtype.RoomType;
import com.staycommerce.domain.roomtype.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class RoomTypeRepositoryImpl implements RoomTypeRepository {

    private final RoomTypeJpaRepository roomTypeJpaRepository;

    @Override
    public RoomType save(RoomType roomType) {
        return roomTypeJpaRepository.save(roomType);
    }

    @Override
    public Optional<RoomType> findById(Long id) {
        return roomTypeJpaRepository.findById(id);
    }

    @Override
    public List<RoomType> findByPropertyId(Long propertyId) {
        return roomTypeJpaRepository.findByPropertyId(propertyId);
    }

    @Override
    public List<RoomType> findByPropertyIdInAndMaxOccupancyGreaterThanEqual(List<Long> propertyIds, int guests) {
        return roomTypeJpaRepository.findByPropertyIdInAndMaxOccupancyGreaterThanEqual(propertyIds, guests);
    }
}
