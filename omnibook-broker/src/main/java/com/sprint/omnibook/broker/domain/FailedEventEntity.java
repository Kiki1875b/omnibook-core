package com.sprint.omnibook.broker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 실패 이벤트 엔티티.
 * 변환 실패한 이벤트를 재처리를 위해 저장.
 */
@Entity
@Table(name = "failed_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FailedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String platform;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(nullable = false)
    private boolean resolved = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public FailedEventEntity(String eventId, String platform, String eventType,
                              String correlationId, String reservationId,
                              String rawPayload, String errorMessage, Instant failedAt) {
        this.eventId = eventId;
        this.platform = platform;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.reservationId = reservationId;
        this.rawPayload = rawPayload;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public void resolve() {
        this.resolved = true;
    }
}
