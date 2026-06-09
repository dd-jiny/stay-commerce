package com.staycommerce.domain.roomtype;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    @Transactional
    public RoomTypeInfo register(Long propertyId, String name, int standardOccupancy,
                                 int maxOccupancy, int extraPersonFee, int totalRoomCount) {
        RoomType roomType = new RoomType(propertyId, name, standardOccupancy, maxOccupancy, extraPersonFee, totalRoomCount);
        return RoomTypeInfo.from(roomTypeRepository.save(roomType));
    }

    @Transactional(readOnly = true)
    public RoomType getById(Long id) {
        return roomTypeRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 객실 타입입니다."));
    }

    @Transactional(readOnly = true)
    public List<RoomType> findByPropertyId(Long propertyId) {
        return roomTypeRepository.findByPropertyId(propertyId);
    }

    /** 검색용: 해당 숙소들 중 guests 를 수용할 수 있는 객실 타입. (가용 재고/요금 조합은 Facade) */
    @Transactional(readOnly = true)
    public List<RoomType> findAccommodatableTypes(List<Long> propertyIds, int guests) {
        return roomTypeRepository.findByPropertyIdInAndMaxOccupancyGreaterThanEqual(propertyIds, guests);
    }
}
