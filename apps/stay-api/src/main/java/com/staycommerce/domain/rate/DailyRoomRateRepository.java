package com.staycommerce.domain.rate;

import java.time.LocalDate;
import java.util.List;

public interface DailyRoomRateRepository {
    DailyRoomRate save(DailyRoomRate rate);
    List<DailyRoomRate> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates);
    List<DailyRoomRate> findByRoomTypeIdInAndDateIn(List<Long> roomTypeIds, List<LocalDate> dates);
}
