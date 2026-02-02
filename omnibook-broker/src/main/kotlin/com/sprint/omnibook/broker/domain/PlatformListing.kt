package com.sprint.omnibook.broker.domain

import com.sprint.omnibook.broker.event.PlatformType
import jakarta.persistence.*
import java.time.Instant

/**
 * OTA 플랫폼 매핑 엔티티.
 * 한 방이 여러 OTA에 등록될 수 있음.
 */
@Entity
@Table(
    name = "platform_listing",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["room_id", "platform_type"]),
        UniqueConstraint(columnNames = ["platform_type", "platform_room_id"])
    ]
)
class PlatformListing(
    @Enumerated(EnumType.STRING)
    @Column(name = "platform_type", nullable = false)
    val platformType: PlatformType,

    @Column(name = "platform_room_id", nullable = false)
    val platformRoomId: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    var room: Room? = null
        internal set

    @Column(name = "is_active", nullable = false)
    var active: Boolean = true
        private set

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

    fun deactivate() {
        this.active = false
    }

    fun activate() {
        this.active = true
    }

    /**
     * 플랫폼 리스팅을 방에 연결한다. (내부 사용 전용)
     */
    internal fun assignRoom(room: Room) {
        this.room = room
    }
}
