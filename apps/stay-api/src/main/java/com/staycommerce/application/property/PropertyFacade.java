package com.staycommerce.application.property;

import com.staycommerce.application.property.PropertyDetailInfo.RoomTypeDetail;
import com.staycommerce.domain.common.PeriodPolicy;
import com.staycommerce.domain.inventory.DailyRoomInventoryService;
import com.staycommerce.domain.property.PropertyInfo;
import com.staycommerce.domain.property.PropertyService;
import com.staycommerce.domain.rate.DailyRoomRate;
import com.staycommerce.domain.rate.DailyRoomRateService;
import com.staycommerce.domain.roomtype.RoomType;
import com.staycommerce.domain.roomtype.RoomTypeService;
import com.staycommerce.domain.wishlist.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 숙소 상세 조회 조립(경량 Application Layer). 단일 숙소에 대해 Property + RoomType +
 * Inventory(가용) + Rate(요금) + 찜 수를 조합한다. F1 검색({@link PropertySearchFacade})의
 * 단일 숙소 버전으로, 기간·인원이 주어지면 객실별 예약 가능 여부·총요금 미리보기까지 산출한다.
 */
@RequiredArgsConstructor
@Component
public class PropertyFacade {

    private final PropertyService propertyService;
    private final RoomTypeService roomTypeService;
    private final DailyRoomInventoryService inventoryService;
    private final DailyRoomRateService rateService;
    private final WishlistService wishlistService;

    /**
     * 기간 미지정 상세: 숙소 메타 + 객실 타입 목록 + 찜 수만 조회한다(가용/요금 미산출).
     */
    @Transactional(readOnly = true)
    public PropertyDetailInfo getPropertyDetail(Long propertyId) {
        PropertyInfo property = propertyService.getById(propertyId);
        long wishCount = wishlistService.countByProperty(propertyId);
        List<RoomTypeDetail> roomTypes = roomTypeService.findByPropertyId(propertyId).stream()
            .map(RoomTypeDetail::ofMetadata)
            .toList();
        return PropertyDetailInfo.of(property, wishCount, roomTypes);
    }

    /**
     * 기간 지정 상세: 객실별 예약 가능 여부와 총요금/1박 평균가 미리보기까지 채운다.
     * 가용 판정은 검색과 동일하게 인원 수용 + 전일자 재고 + 전일자 요금을 모두 만족해야 한다.
     */
    @Transactional(readOnly = true)
    public PropertyDetailInfo getPropertyDetail(Long propertyId, LocalDate checkIn, LocalDate checkOut, int guests) {
        PeriodPolicy.validate(checkIn, checkOut);
        List<LocalDate> dates = checkIn.datesUntil(checkOut).toList();
        int nights = dates.size();

        PropertyInfo property = propertyService.getById(propertyId);
        long wishCount = wishlistService.countByProperty(propertyId);

        List<RoomType> roomTypes = roomTypeService.findByPropertyId(propertyId);
        List<Long> roomTypeIds = roomTypes.stream().map(RoomType::getId).toList();
        Map<Long, List<DailyRoomRate>> ratesByType = rateService.findRatesByRoomType(roomTypeIds, dates);

        List<RoomTypeDetail> details = roomTypes.stream()
            .map(roomType -> toPreview(roomType, ratesByType.get(roomType.getId()), dates, nights, guests))
            .toList();

        return PropertyDetailInfo.of(property, wishCount, details);
    }

    private RoomTypeDetail toPreview(RoomType roomType, List<DailyRoomRate> rates,
                                     List<LocalDate> dates, int nights, int guests) {
        boolean ratesComplete = rates != null && rates.size() == nights;
        boolean available = roomType.canAccommodate(guests)
            && inventoryService.isAllAvailable(roomType.getId(), dates)
            && ratesComplete;
        if (!available) {
            return RoomTypeDetail.ofPreview(roomType, false, null, null);
        }
        int basePrice = rates.stream().mapToInt(DailyRoomRate::getPrice).sum();
        int totalAmount = basePrice + roomType.extraFeeFor(guests) * nights;
        return RoomTypeDetail.ofPreview(roomType, true, totalAmount, totalAmount / nights);
    }
}
