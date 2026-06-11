package com.staycommerce.domain.inventory;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "daily_room_inventory",
    uniqueConstraints = @UniqueConstraint(name = "uk_inventory_room_date",
        columnNames = {"room_type_id", "date"}))
public class DailyRoomInventory extends BaseEntity {

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int held;

    protected DailyRoomInventory() {}

    public DailyRoomInventory(Long roomTypeId, LocalDate date, int stock) {
        if (roomTypeId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "객실 타입 ID는 필수입니다.");
        }
        if (date == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "날짜는 필수입니다.");
        }
        if (stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
        this.roomTypeId = roomTypeId;
        this.date = date;
        this.stock = stock;
        this.held = 0;
    }

    /** 가용 수량 = stock - held. */
    public int availableCount() {
        return stock - held;
    }

    /** 임시 점유 (PENDING 예약 생성). 가드: 가용 >= 1. */
    public void hold() {
        if (availableCount() < 1) {
            throw new CoreException(ErrorType.CONFLICT, "잔여 재고가 없습니다.");
        }
        this.held += 1;
    }

    /** 임시 점유 해제 (PENDING 취소/만료). */
    public void releaseHold() {
        if (held < 1) {
            throw new CoreException(ErrorType.CONFLICT, "해제할 홀딩이 없습니다.");
        }
        this.held -= 1;
    }

    /** 확정 (PENDING → CONFIRMED). held → stock 이전. */
    public void convertHoldToStock() {
        if (held < 1 || stock < 1) {
            throw new CoreException(ErrorType.CONFLICT, "확정할 재고가 없습니다.");
        }
        this.held -= 1;
        this.stock -= 1;
    }

    /** 실재고 복원 (CONFIRMED 취소). */
    public void restoreStock() {
        this.stock += 1;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getStock() {
        return stock;
    }

    public int getHeld() {
        return held;
    }
}
