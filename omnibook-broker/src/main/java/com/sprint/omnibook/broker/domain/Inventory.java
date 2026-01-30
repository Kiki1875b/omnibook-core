package com.sprint.omnibook.broker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 날짜별 재고 엔티티.
 * 없으면 AVAILABLE로 간주.
 */
@Entity
@Table(name = "inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status = InventoryStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Inventory(Room room, LocalDate date) {
        this.room = room;
        this.date = date;
        this.status = InventoryStatus.AVAILABLE;
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

    public void book(Reservation reservation) {
        this.status = InventoryStatus.BOOKED;
        this.reservation = reservation;
        this.blockReason = null;
    }

    public void block(String reason) {
        this.status = InventoryStatus.BLOCKED;
        this.reservation = null;
        this.blockReason = reason;
    }

    public void release() {
        this.status = InventoryStatus.AVAILABLE;
        this.reservation = null;
        this.blockReason = null;
    }

    public boolean isAvailable() {
        return this.status == InventoryStatus.AVAILABLE;
    }
}
