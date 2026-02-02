package com.sprint.omnibook.broker.domain

import jakarta.persistence.*
import java.time.Instant

/**
 * 방 엔티티.
 */
@Entity
@Table(name = "room")
class Room(
    @Column(nullable = false)
    var name: String,

    @Column(name = "room_type")
    var roomType: String? = null,

    var capacity: Int? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    var property: Property? = null
        internal set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RoomStatus = RoomStatus.ACTIVE

    @OneToMany(mappedBy = "room", cascade = [CascadeType.ALL])
    val platformListings: MutableList<PlatformListing> = mutableListOf()

    @OneToMany(mappedBy = "room", cascade = [CascadeType.ALL])
    val reservations: MutableList<Reservation> = mutableListOf()

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

    fun addPlatformListing(listing: PlatformListing) {
        platformListings.add(listing)
        listing.assignRoom(this)
    }

    /**
     * 방을 숙소에 연결한다. (내부 사용 전용)
     */
    internal fun assignProperty(property: Property) {
        this.property = property
    }
}
