package com.staycommerce.infrastructure.inventory;

import com.staycommerce.domain.inventory.DailyRoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyRoomInventoryJpaRepository extends JpaRepository<DailyRoomInventory, Long> {

    List<DailyRoomInventory> findByRoomTypeIdAndDateIn(Long roomTypeId, List<LocalDate> dates);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE daily_room_inventory SET held = held + 1
         WHERE room_type_id = :rtId AND date = :date AND stock - held >= 1
        """, nativeQuery = true)
    int hold(@Param("rtId") Long roomTypeId, @Param("date") LocalDate date);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE daily_room_inventory SET held = held - 1
         WHERE room_type_id = :rtId AND date = :date AND held >= 1
        """, nativeQuery = true)
    int releaseHold(@Param("rtId") Long roomTypeId, @Param("date") LocalDate date);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE daily_room_inventory SET held = held - 1, stock = stock - 1
         WHERE room_type_id = :rtId AND date = :date AND held >= 1 AND stock >= 1
        """, nativeQuery = true)
    int convertHoldToStock(@Param("rtId") Long roomTypeId, @Param("date") LocalDate date);

    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE daily_room_inventory SET stock = stock + 1
         WHERE room_type_id = :rtId AND date = :date
        """, nativeQuery = true)
    int restoreStock(@Param("rtId") Long roomTypeId, @Param("date") LocalDate date);
}
