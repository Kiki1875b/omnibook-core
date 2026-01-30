package com.sprint.omnibook.broker.domain;

import com.sprint.omnibook.broker.event.PlatformType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 예약 엔티티.
 */
@Entity
@Table(name = "reservation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform_type", "platform_reservation_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    private PlatformType platformType;

    @Column(name = "platform_reservation_id", nullable = false)
    private String platformReservationId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_phone")
    private String guestPhone;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    private List<Inventory> inventories = new ArrayList<>();

    @OneToMany(mappedBy = "reservation")
    private List<ReservationEventEntity> events = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Reservation(PlatformType platformType, String platformReservationId,
                       LocalDate checkIn, LocalDate checkOut,
                       String guestName, String guestPhone, String guestEmail,
                       BigDecimal totalAmount, ReservationStatus status, Instant bookedAt) {
        this.platformType = platformType;
        this.platformReservationId = platformReservationId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.bookedAt = bookedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public void markNoShow() {
        this.status = ReservationStatus.NOSHOW;
    }

    public void updateGuestInfo(String guestName, String guestPhone, String guestEmail) {
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.guestEmail = guestEmail;
    }
}
