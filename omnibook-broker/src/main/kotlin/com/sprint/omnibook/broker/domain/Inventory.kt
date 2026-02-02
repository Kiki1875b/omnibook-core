package com.sprint.omnibook.broker.domain

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

/**
 * 날짜별 재고 엔티티.
 * 없으면 AVAILABLE로 간주.
 */
@Entity
@Table(
    name = "inventory",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["room_id", "date"])
    ]
)
class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: Room? = null
        private set

    @Column(nullable = false)
    var date: LocalDate? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: InventoryStatus = InventoryStatus.AVAILABLE
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    var reservation: Reservation? = null
        private set

    @Column(name = "block_reason")
    var blockReason: String? = null
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun book(reservation: Reservation?) {
        this.status = InventoryStatus.BOOKED
        this.reservation = reservation
        this.blockReason = null
    }

    fun block(reason: String) {
        this.status = InventoryStatus.BLOCKED
        this.reservation = null
        this.blockReason = reason
    }

    fun release() {
        this.status = InventoryStatus.AVAILABLE
        this.reservation = null
        this.blockReason = null
    }

    fun isAvailable(): Boolean = this.status == InventoryStatus.AVAILABLE

    companion object {
        /**
         * 예약된 재고 생성 팩토리 메서드.
         * 새 재고는 BOOKED 상태로 시작한다.
         */
        fun createBooked(room: Room, date: LocalDate): Inventory {
            return Inventory().apply {
                this.room = room
                this.date = date
                this.status = InventoryStatus.BOOKED
            }
        }
    }
}
