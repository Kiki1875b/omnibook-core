package com.sprint.omnibook.broker.domain

import jakarta.persistence.*
import java.time.Instant

/**
 * 숙소 엔티티.
 */
@Entity
@Table(name = "property")
class Property(
    @Column(nullable = false)
    var name: String,

    var address: String? = null
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

    @OneToMany(mappedBy = "property", cascade = [CascadeType.ALL])
    val rooms: MutableList<Room> = mutableListOf()

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

    fun addRoom(room: Room) {
        rooms.add(room)
        room.assignProperty(this)
    }
}
