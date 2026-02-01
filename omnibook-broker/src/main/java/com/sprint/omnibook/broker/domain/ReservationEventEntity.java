package com.sprint.omnibook.broker.domain;

import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 정규화된 이벤트 엔티티.
 * OTA에서 수신한 이벤트를 정규화하여 저장.
 */
@Entity
@Table(name = "reservation_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    private PlatformType platformType;

    @Column(name = "platform_reservation_id", nullable = false)
    private String platformReservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "property_name")
    private String propertyName;

    @Column(name = "property_address")
    private String propertyAddress;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public ReservationEventEntity(UUID eventId, PlatformType platformType, String platformReservationId,
                                   EventType eventType, LocalDate checkIn, LocalDate checkOut,
                                   String guestName, String guestPhone, String guestEmail,
                                   BigDecimal totalAmount, String status,
                                   String propertyName, String propertyAddress,
                                   Instant occurredAt, Instant receivedAt) {
        this.eventId = eventId;
        this.platformType = platformType;
        this.platformReservationId = platformReservationId;
        this.eventType = eventType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.propertyName = propertyName;
        this.propertyAddress = propertyAddress;
        this.occurredAt = occurredAt;
        this.receivedAt = receivedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public void markProcessed(Room room, Reservation reservation) {
        this.processed = true;
        this.processedAt = Instant.now();
        this.room = room;
        this.reservation = reservation;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.processed = false;
        this.errorMessage = errorMessage;
    }
}
