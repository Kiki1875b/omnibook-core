package com.sprint.omnibook.broker.domain.repository

import com.sprint.omnibook.broker.domain.PlatformListing
import com.sprint.omnibook.broker.event.PlatformType
import org.springframework.data.jpa.repository.JpaRepository

interface PlatformListingRepository : JpaRepository<PlatformListing, Long> {

    /**
     * OTA 플랫폼과 플랫폼 방 ID로 매핑 조회.
     */
    fun findByPlatformTypeAndPlatformRoomId(platformType: PlatformType, platformRoomId: String): PlatformListing?
}
