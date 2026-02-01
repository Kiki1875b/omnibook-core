package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.Inventory;
import com.sprint.omnibook.broker.domain.InventoryStatus;
import com.sprint.omnibook.broker.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 특정 방의 날짜 범위 재고 조회.
     */
    List<Inventory> findByRoomAndDateBetween(Room room, LocalDate startDate, LocalDate endDate);

    /**
     * 특정 방의 날짜 범위 재고 조회 (비관적 락).
     * 동시성 제어를 위해 SELECT FOR UPDATE 사용.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.room = :room AND i.date >= :startDate AND i.date < :endDate")
    List<Inventory> findByRoomAndDateRangeForUpdate(
            @Param("room") Room room,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 방의 날짜 범위에서 AVAILABLE이 아닌 재고 조회.
     * 예약 가능 여부 판별용.
     */
    @Query("SELECT i FROM Inventory i WHERE i.room = :room AND i.date >= :startDate AND i.date < :endDate AND i.status != :availableStatus")
    List<Inventory> findUnavailableByRoomAndDateRange(
            @Param("room") Room room,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("availableStatus") InventoryStatus availableStatus);

    /**
     * 특정 방, 특정 날짜의 재고 조회.
     */
    java.util.Optional<Inventory> findByRoomAndDate(Room room, LocalDate date);
}
