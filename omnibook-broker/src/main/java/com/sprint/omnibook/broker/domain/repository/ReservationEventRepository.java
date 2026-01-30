package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.ReservationEventEntity;
import com.sprint.omnibook.broker.event.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationEventRepository extends JpaRepository<ReservationEventEntity, Long> {

    /**
     * 이벤트 ID로 조회.
     */
    Optional<ReservationEventEntity> findByEventId(UUID eventId);

    /**
     * 미처리 이벤트 조회.
     */
    List<ReservationEventEntity> findByProcessedFalseOrderByReceivedAtAsc();

    /**
     * 플랫폼과 예약 ID로 이벤트 조회.
     */
    List<ReservationEventEntity> findByPlatformTypeAndPlatformReservationIdOrderByReceivedAtDesc(
            PlatformType platformType, String platformReservationId);
}
