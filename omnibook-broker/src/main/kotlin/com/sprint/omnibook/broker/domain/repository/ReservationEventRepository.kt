package com.sprint.omnibook.broker.domain.repository

import com.sprint.omnibook.broker.domain.ReservationEventEntity
import com.sprint.omnibook.broker.event.PlatformType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReservationEventRepository : JpaRepository<ReservationEventEntity, Long> {

    /**
     * 이벤트 ID로 조회.
     */
    fun findByEventId(eventId: UUID): ReservationEventEntity?

    /**
     * 미처리 이벤트 조회.
     */
    fun findByProcessedFalseOrderByReceivedAtAsc(): List<ReservationEventEntity>

    /**
     * 플랫폼과 예약 ID로 이벤트 조회.
     */
    fun findByPlatformTypeAndPlatformReservationIdOrderByReceivedAtDesc(
        platformType: PlatformType,
        platformReservationId: String
    ): List<ReservationEventEntity>
}
