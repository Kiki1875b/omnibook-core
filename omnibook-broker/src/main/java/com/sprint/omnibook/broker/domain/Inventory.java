package com.sprint.omnibook.broker.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /**
     * 가용 재고 생성 팩토리 메서드.
     * 새 재고는 항상 AVAILABLE 상태로 시작한다.
     */
    public static Inventory createBooked(Room room, LocalDate date) {
        Inventory inventory = new Inventory();
        inventory.room = room;
        inventory.date = date;
        inventory.status = InventoryStatus.BOOKED;
        return inventory;
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
