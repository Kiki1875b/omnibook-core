package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.PlatformListing;
import com.sprint.omnibook.broker.event.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformListingRepository extends JpaRepository<PlatformListing, Long> {

    /**
     * OTA 플랫폼과 플랫폼 방 ID로 매핑 조회.
     */
    Optional<PlatformListing> findByPlatformTypeAndPlatformRoomId(PlatformType platformType, String platformRoomId);

    /**
     * OTA 플랫폼과 플랫폼 숙소 ID로 매핑 조회.
     */
    Optional<PlatformListing> findByPlatformTypeAndPlatformPropertyId(PlatformType platformType, String platformPropertyId);
}
