package com.staycommerce.domain.rate;

import com.staycommerce.domain.BaseEntity;
import com.staycommerce.support.error.CoreException;
import com.staycommerce.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "daily_room_rate",
    uniqueConstraints = @UniqueConstraint(name = "uk_rate_room_date",
        columnNames = {"room_type_id", "date"}))
public class DailyRoomRate extends BaseEntity {

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int price;

    protected DailyRoomRate() {}

    public DailyRoomRate(Long roomTypeId, LocalDate date, int price) {
        if (roomTypeId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "객실 타입 ID는 필수입니다.");
        }
        if (date == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "날짜는 필수입니다.");
        }
        if (price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "요금은 0 이상이어야 합니다.");
        }
        this.roomTypeId = roomTypeId;
        this.date = date;
        this.price = price;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getPrice() {
        return price;
    }
}
