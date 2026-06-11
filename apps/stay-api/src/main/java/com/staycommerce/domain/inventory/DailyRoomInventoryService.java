package com.staycommerce.domain.inventory;

import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Component
public class DailyRoomInventoryService {

    private final DailyRoomInventoryRepository inventoryRepository;

    /**
     * 다일자 홀딩. date ASC 순으로 처리(데드락 회피). 한 날짜라도 재고 부족이면 CONFLICT,
     * 트랜잭션 롤백으로 이미 hold 된 날짜도 자동 복원된다.
     */
    @Transactional
    public void hold(Long roomTypeId, List<LocalDate> dates) {
        for (LocalDate date : sortedAsc(dates)) {
            if (inventoryRepository.hold(roomTypeId, date) != 1) {
                throw new CoreException(ErrorType.CONFLICT, "해당 기간의 재고가 부족합니다. (" + date + ")");
            }
        }
    }

    /** 다일자 홀딩 해제 (PENDING 취소/만료). best-effort. */
    @Transactional
    public void releaseHold(Long roomTypeId, List<LocalDate> dates) {
        for (LocalDate date : sortedAsc(dates)) {
            inventoryRepository.releaseHold(roomTypeId, date);
        }
    }

    /** 다일자 확정 (held → stock). 한 날짜라도 실패하면 CONFLICT. */
    @Transactional
    public void convertHoldToStock(Long roomTypeId, List<LocalDate> dates) {
        for (LocalDate date : sortedAsc(dates)) {
            if (inventoryRepository.convertHoldToStock(roomTypeId, date) != 1) {
                throw new CoreException(ErrorType.CONFLICT, "확정 처리에 실패했습니다. (" + date + ")");
            }
        }
    }

    /** 다일자 실재고 복원 (CONFIRMED 취소). best-effort. */
    @Transactional
    public void restoreStock(Long roomTypeId, List<LocalDate> dates) {
        for (LocalDate date : sortedAsc(dates)) {
            inventoryRepository.restoreStock(roomTypeId, date);
        }
    }

    /** 검색용: 모든 날짜에 가용(stock - held >= 1)인지. 누락 일자가 있으면 false. */
    @Transactional(readOnly = true)
    public boolean isAllAvailable(Long roomTypeId, List<LocalDate> dates) {
        List<DailyRoomInventory> rows = inventoryRepository.findByRoomTypeIdAndDateIn(roomTypeId, dates);
        if (rows.size() != dates.size()) {
            return false;
        }
        return rows.stream().allMatch(r -> r.availableCount() >= 1);
    }

    private List<LocalDate> sortedAsc(List<LocalDate> dates) {
        return dates.stream().sorted().toList();
    }
}
