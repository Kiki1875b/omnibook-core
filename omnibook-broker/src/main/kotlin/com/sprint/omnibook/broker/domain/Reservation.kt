package com.sprint.omnibook.broker.domain

import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.event.ReservationEvent
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * 예약 엔티티.
 */
@Entity
@Table(
    name = "reservation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["platform_type", "platform_reservation_id"])
    ]
)
class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: Room? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    var platformType: PlatformType? = null
        private set

    @Column(name = "platform_reservation_id", nullable = false)
    var platformReservationId: String? = null
        private set

    @Column(name = "check_in", nullable = false)
    var checkIn: LocalDate? = null
        private set

    @Column(name = "check_out", nullable = false)
    var checkOut: LocalDate? = null
        private set

    @Column(name = "guest_name")
    var guestName: String? = null
        private set

    @Column(name = "guest_phone")
    var guestPhone: String? = null
        private set

    @Column(name = "guest_email")
    var guestEmail: String? = null
        private set

    @Column(name = "total_amount")
    var totalAmount: BigDecimal? = null
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus? = null
        private set

    @Column(name = "booked_at")
    var bookedAt: Instant? = null
        private set

    @OneToMany(mappedBy = "reservation", cascade = [CascadeType.ALL])
    val inventories: MutableList<Inventory> = mutableListOf()

    @OneToMany(mappedBy = "reservation")
    val events: MutableList<ReservationEventEntity> = mutableListOf()

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

    fun cancel() {
        this.status = ReservationStatus.CANCELLED
    }

    fun complete() {
        this.status = ReservationStatus.COMPLETED
    }

    fun markNoShow() {
        this.status = ReservationStatus.NOSHOW
    }

    fun updateGuestInfo(guestName: String?, guestPhone: String?, guestEmail: String?) {
        this.guestName = guestName
        this.guestPhone = guestPhone
        this.guestEmail = guestEmail
    }

    companion object {
        /**
         * 예약 생성 팩토리 메서드.
         * 새 예약은 항상 CONFIRMED 상태로 시작한다.
         */
        fun book(room: Room, event: ReservationEvent): Reservation {
            return Reservation().apply {
                this.room = room
                this.platformType = event.platformType
                this.platformReservationId = event.platformReservationId
                this.checkIn = event.checkIn
                this.checkOut = event.checkOut
                this.guestName = event.guestName
                this.guestPhone = event.guestPhone
                this.guestEmail = event.guestEmail
                this.totalAmount = event.totalAmount
                this.status = ReservationStatus.CONFIRMED
                this.bookedAt = event.occurredAt
            }
        }
    }
}
