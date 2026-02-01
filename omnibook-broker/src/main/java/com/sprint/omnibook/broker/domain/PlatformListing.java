package com.sprint.omnibook.broker.domain;

import com.sprint.omnibook.broker.event.PlatformType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * OTA 플랫폼 매핑 엔티티.
 * 한 방이 여러 OTA에 등록될 수 있음.
 */
@Entity
@Table(name = "platform_listing", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "platform_type"}),
        @UniqueConstraint(columnNames = {"platform_type", "platform_room_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    private PlatformType platformType;

    @Column(name = "platform_room_id", nullable = false)
    private String platformRoomId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public PlatformListing(PlatformType platformType, String platformRoomId) {
        this.platformType = platformType;
        this.platformRoomId = platformRoomId;
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

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /**
     * 플랫폼 리스팅을 방에 연결한다. (내부 사용 전용)
     */
    void assignRoom(Room room) {
        this.room = room;
    }
}
