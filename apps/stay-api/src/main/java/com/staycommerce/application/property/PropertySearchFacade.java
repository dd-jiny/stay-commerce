package com.staycommerce.application.property;

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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 숙소 검색 조립(경량 Application Layer). 여러 도메인 서비스를 점진 필터링으로 좁혀
 * 기간 총요금/1박 평균가를 계산한다. 계산·조립은 Facade, 규칙은 도메인에 위임.
 *
 * <p>점진 필터링: Property(도시) → RoomType(인원 수용) → Inventory(전일자 가용) → Rate(전일자 요금).
 */
@RequiredArgsConstructor
@Component
public class PropertySearchFacade {

    private final PropertyService propertyService;
    private final RoomTypeService roomTypeService;
    private final DailyRoomInventoryService inventoryService;
    private final DailyRoomRateService rateService;
    private final WishlistService wishlistService;

    @Transactional(readOnly = true)
    public List<PropertySearchInfo> search(PropertySearchCriteria criteria) {
        PeriodPolicy.validate(criteria.checkIn(), criteria.checkOut());
        List<LocalDate> dates = criteria.checkIn().datesUntil(criteria.checkOut()).toList();
        int nights = dates.size();

        // 1. 도시로 1차 필터
        List<PropertyInfo> properties = propertyService.findByCity(criteria.city());
        if (properties.isEmpty()) {
            return List.of();
        }
        Map<Long, PropertyInfo> propertyById = properties.stream()
            .collect(Collectors.toMap(PropertyInfo::id, Function.identity()));
        List<Long> propertyIds = new ArrayList<>(propertyById.keySet());

        // 2. 인원 수용 가능한 객실 타입만
        List<RoomType> accommodatable = roomTypeService.findAccommodatableTypes(propertyIds, criteria.guests());
        if (accommodatable.isEmpty()) {
            return List.of();
        }

        // 3. 전일자 가용 재고가 있어야 통과 (누락 일자 있으면 제외)
        List<RoomType> available = accommodatable.stream()
            .filter(roomType -> inventoryService.isAllAvailable(roomType.getId(), dates))
            .toList();
        if (available.isEmpty()) {
            return List.of();
        }

        // 4. 전일자 요금을 한 번에 조회. 요금 행이 박수만큼 갖춰진 객실만 통과(누락 → 제외).
        //    (예외를 트랜잭션 경계 넘는 제어 흐름으로 쓰지 않도록 배치 조회 후 직접 판별)
        List<Long> availableIds = available.stream().map(RoomType::getId).toList();
        Map<Long, List<DailyRoomRate>> ratesByType = rateService.findRatesByRoomType(availableIds, dates);

        // 찜 수는 숙소 단위로 한 번에 조회 (0건 숙소는 Map에 없어 getOrDefault로 보정)
        Map<Long, Long> wishCounts = wishlistService.countByProperties(propertyIds);

        List<PropertySearchInfo> result = new ArrayList<>();
        for (RoomType roomType : available) {
            List<DailyRoomRate> rates = ratesByType.get(roomType.getId());
            if (rates == null || rates.size() != nights) {
                continue;
            }
            int basePrice = rates.stream().mapToInt(DailyRoomRate::getPrice).sum();
            int totalAmount = basePrice + roomType.extraFeeFor(criteria.guests()) * nights;
            PropertyInfo property = propertyById.get(roomType.getPropertyId());
            result.add(PropertySearchInfo.of(
                property, roomType,
                totalAmount, totalAmount / nights,
                wishCounts.getOrDefault(property.id(), 0L)));
        }

        return sort(result, criteria.sortOrDefault());
    }

    private List<PropertySearchInfo> sort(List<PropertySearchInfo> infos, PropertySearchSort sort) {
        Comparator<PropertySearchInfo> comparator = switch (sort) {
            case PRICE_ASC -> Comparator.comparingInt(PropertySearchInfo::totalAmount);
            // RECOMMENDED는 정책 미정으로 찜 수 내림차순으로 갈음
            case WISHES_DESC, RECOMMENDED -> Comparator.comparingLong(PropertySearchInfo::wishCount).reversed();
        };
        return infos.stream().sorted(comparator).toList();
    }
}
