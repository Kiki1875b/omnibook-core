package com.sprint.omnibook.broker.domain

import jakarta.persistence.*
import java.time.Instant

/**
 * 실패 이벤트 엔티티.
 * 변환 실패한 이벤트를 재처리를 위해 저장.
 */
@Entity
@Table(name = "failed_event")
class FailedEventEntity(
    @Column(name = "event_id", nullable = false)
    val eventId: String,

    @Column(nullable = false)
    val platform: String,

    @Column(name = "event_type")
    val eventType: String?,

    @Column(name = "correlation_id")
    val correlationId: String?,

    @Column(name = "reservation_id")
    val reservationId: String?,

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    val rawPayload: String?,

    @Column(name = "error_message", columnDefinition = "TEXT")
    val errorMessage: String?,

    @Column(name = "failed_at", nullable = false)
    val failedAt: Instant
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0
        private set

    @Column(nullable = false)
    var resolved: Boolean = false
        private set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @PrePersist
    protected fun onCreate() {
        createdAt = Instant.now()
    }

    fun incrementRetryCount() {
        this.retryCount++
    }

    fun resolve() {
        this.resolved = true
    }
}
