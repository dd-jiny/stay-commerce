package com.staycommerce.infrastructure.inventory;

import com.staycommerce.domain.inventory.DailyRoomInventory;
import com.staycommerce.domain.inventory.DailyRoomInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DailyRoomInventoryRepositoryImpl implements DailyRoomInventoryRepository {

    private final DailyRoomInventoryJpaRepository jpaRepository;

    @Override
    public DailyRoomInventory save(DailyRoomInventory inventory) {
        return jpaRepository.save(inventory);
    }

    @Override
    public List<DailyRoomInventory> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates) {
        return jpaRepository.findByRoomTypeIdAndDateIn(roomTypeId, dates);
    }

    @Override
    public int hold(Long roomTypeId, LocalDate date) {
        return jpaRepository.hold(roomTypeId, date);
    }

    @Override
    public int releaseHold(Long roomTypeId, LocalDate date) {
        return jpaRepository.releaseHold(roomTypeId, date);
    }

    @Override
    public int convertHoldToStock(Long roomTypeId, LocalDate date) {
        return jpaRepository.convertHoldToStock(roomTypeId, date);
    }

    @Override
    public int restoreStock(Long roomTypeId, LocalDate date) {
        return jpaRepository.restoreStock(roomTypeId, date);
    }
}
