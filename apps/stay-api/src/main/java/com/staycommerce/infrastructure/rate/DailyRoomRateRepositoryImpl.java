package com.staycommerce.infrastructure.rate;

import com.staycommerce.domain.rate.DailyRoomRate;
import com.staycommerce.domain.rate.DailyRoomRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DailyRoomRateRepositoryImpl implements DailyRoomRateRepository {

    private final DailyRoomRateJpaRepository dailyRoomRateJpaRepository;

    @Override
    public DailyRoomRate save(DailyRoomRate rate) {
        return dailyRoomRateJpaRepository.save(rate);
    }

    @Override
    public List<DailyRoomRate> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates) {
        return dailyRoomRateJpaRepository.findByRoomTypeIdAndDateIn(roomTypeId, dates);
    }

    @Override
    public List<DailyRoomRate> findByRoomTypeIdInAndDateIn(List<Long> roomTypeIds, List<LocalDate> dates) {
        return dailyRoomRateJpaRepository.findByRoomTypeIdInAndDateIn(roomTypeIds, dates);
    }
}
