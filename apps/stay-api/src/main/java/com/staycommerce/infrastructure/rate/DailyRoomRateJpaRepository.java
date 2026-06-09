package com.staycommerce.infrastructure.rate;

import com.staycommerce.domain.rate.DailyRoomRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyRoomRateJpaRepository extends JpaRepository<DailyRoomRate, Long> {
    List<DailyRoomRate> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates);
    List<DailyRoomRate> findByRoomTypeIdInAndDateIn(List<Long> roomTypeIds, List<LocalDate> dates);
}
