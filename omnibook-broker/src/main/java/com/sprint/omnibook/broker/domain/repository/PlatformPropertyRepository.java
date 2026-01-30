package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.PlatformProperty;
import com.sprint.omnibook.broker.event.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformPropertyRepository extends JpaRepository<PlatformProperty, Long> {

    /**
     * OTA 플랫폼과 플랫폼 숙소 ID로 매핑 조회.
     */
    Optional<PlatformProperty> findByPlatformTypeAndPlatformPropertyId(
            PlatformType platformType, String platformPropertyId);
}
