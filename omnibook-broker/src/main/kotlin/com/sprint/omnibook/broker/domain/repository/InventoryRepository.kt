package com.sprint.omnibook.broker.domain.repository

import com.sprint.omnibook.broker.domain.Inventory
import com.sprint.omnibook.broker.domain.InventoryStatus
import com.sprint.omnibook.broker.domain.Room
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface InventoryRepository : JpaRepository<Inventory, Long> {

    /**
     * 특정 방의 날짜 범위 재고 조회.
     */
    fun findByRoomAndDateBetween(room: Room, startDate: LocalDate, endDate: LocalDate): List<Inventory>

    /**
     * 특정 방의 날짜 범위 재고 조회 (비관적 락).
     * 동시성 제어를 위해 SELECT FOR UPDATE 사용.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.room = :room AND i.date >= :startDate AND i.date < :endDate")
    fun findByRoomAndDateRangeForUpdate(
        @Param("room") room: Room,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Inventory>

    /**
     * 특정 방의 날짜 범위에서 AVAILABLE이 아닌 재고 조회.
     * 예약 가능 여부 판별용.
     */
    @Query("SELECT i FROM Inventory i WHERE i.room = :room AND i.date >= :startDate AND i.date < :endDate AND i.status != :availableStatus")
    fun findUnavailableByRoomAndDateRange(
        @Param("room") room: Room,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        @Param("availableStatus") availableStatus: InventoryStatus
    ): List<Inventory>

    /**
     * 특정 방, 특정 날짜의 재고 조회.
     */
    fun findByRoomAndDate(room: Room, date: LocalDate): Inventory?
}
