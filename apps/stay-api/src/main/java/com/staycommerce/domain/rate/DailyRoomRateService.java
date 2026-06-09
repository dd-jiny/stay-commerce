package com.staycommerce.domain.rate;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class DailyRoomRateService {

    private final DailyRoomRateRepository rateRepository;

    @Transactional
    public DailyRoomRate register(Long roomTypeId, LocalDate date, int price) {
        return rateRepository.save(new DailyRoomRate(roomTypeId, date, price));
    }

    /**
     * 기간 기본요금 합산. 기간 내 한 날짜라도 요금 행이 누락되면 CONFLICT.
     * (추가 인원료는 포함하지 않음 — RoomType.extraFeeFor 책임)
     */
    @Transactional(readOnly = true)
    public int sumBasePrice(Long roomTypeId, List<LocalDate> dates) {
        List<DailyRoomRate> rates = rateRepository.findByRoomTypeIdAndDateIn(roomTypeId, dates);
        if (rates.size() != dates.size()) {
            throw new CoreException(ErrorType.CONFLICT, "해당 기간의 요금 정보가 일부 누락되었습니다.");
        }
        return rates.stream().mapToInt(DailyRoomRate::getPrice).sum();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<DailyRoomRate>> findRatesByRoomType(List<Long> roomTypeIds, List<LocalDate> dates) {
        return rateRepository.findByRoomTypeIdInAndDateIn(roomTypeIds, dates).stream()
            .collect(Collectors.groupingBy(DailyRoomRate::getRoomTypeId));
    }
}
