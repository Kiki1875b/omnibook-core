package com.sprint.omnibook.broker.domain;

import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /**
     * 예약 생성 팩토리 메서드.
     * 새 예약은 항상 CONFIRMED 상태로 시작한다.
     */
    public static Reservation book(Room room, ReservationEvent event) {
        Reservation reservation = new Reservation();
        reservation.room = room;
        reservation.platformType = event.getPlatformType();
        reservation.platformReservationId = event.getPlatformReservationId();
        reservation.checkIn = event.getCheckIn();
        reservation.checkOut = event.getCheckOut();
        reservation.guestName = event.getGuestName();
        reservation.guestPhone = event.getGuestPhone();
        reservation.guestEmail = event.getGuestEmail();
        reservation.totalAmount = event.getTotalAmount();
        reservation.status = ReservationStatus.CONFIRMED;
        reservation.bookedAt = event.getOccurredAt();
        return reservation;
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
