package com.sprint.omnibook.broker.domain.repository

import com.sprint.omnibook.broker.domain.Reservation
import com.sprint.omnibook.broker.event.PlatformType
import org.springframework.data.jpa.repository.JpaRepository

interface ReservationRepository : JpaRepository<Reservation, Long> {

    /**
     * OTA 플랫폼과 플랫폼 예약 ID로 예약 조회.
     */
    fun findByPlatformTypeAndPlatformReservationId(platformType: PlatformType, platformReservationId: String): Reservation?
}
