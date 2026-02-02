package com.sprint.omnibook.broker.domain

import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 정규화된 이벤트 엔티티.
 * OTA에서 수신한 이벤트를 정규화하여 저장.
 */
@Entity
@Table(name = "reservation_event")
class ReservationEventEntity(
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    val platformType: PlatformType,

    @Column(name = "platform_reservation_id", nullable = false)
    val platformReservationId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    val eventType: EventType,

    @Column(name = "check_in")
    val checkIn: LocalDate?,

    @Column(name = "check_out")
    val checkOut: LocalDate?,

    @Column(name = "guest_name")
    val guestName: String?,

    @Column(name = "guest_phone")
    val guestPhone: String?,

    @Column(name = "guest_email")
    val guestEmail: String?,

    @Column(name = "total_amount")
    val totalAmount: BigDecimal?,

    @Column(name = "status")
    val status: String?,

    @Column(name = "property_name")
    val propertyName: String?,

    @Column(name = "property_address")
    val propertyAddress: String?,

    @Column(name = "occurred_at")
    val occurredAt: Instant?,

    @Column(name = "received_at", nullable = false)
    val receivedAt: Instant
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    var room: Room? = null
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    var reservation: Reservation? = null
        private set

    @Column(nullable = false)
    var processed: Boolean = false
        private set

    val isProcessed: Boolean get() = processed

    @Column(name = "processed_at")
    var processedAt: Instant? = null
        private set

    @Column(name = "error_message")
    var errorMessage: String? = null
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }

    fun markProcessed(room: Room, reservation: Reservation) {
        this.processed = true
        this.processedAt = Instant.now()
        this.room = room
        this.reservation = reservation
        this.errorMessage = null
    }

    fun markFailed(errorMessage: String) {
        this.processed = false
        this.errorMessage = errorMessage
    }
}
