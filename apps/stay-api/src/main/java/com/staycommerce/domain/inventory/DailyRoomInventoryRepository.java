package com.staycommerce.domain.inventory;

import java.time.LocalDate;
import java.util.List;

public interface DailyRoomInventoryRepository {
    DailyRoomInventory save(DailyRoomInventory inventory);
    List<DailyRoomInventory> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates);

    /** 원자적 UPDATE. 반환값은 영향 행 수(0 또는 1). */
    int hold(Long roomTypeId, LocalDate date);
    int releaseHold(Long roomTypeId, LocalDate date);
    int convertHoldToStock(Long roomTypeId, LocalDate date);
    int restoreStock(Long roomTypeId, LocalDate date);
}
