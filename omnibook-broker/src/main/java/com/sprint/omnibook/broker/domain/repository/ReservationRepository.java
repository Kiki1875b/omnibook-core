package com.sprint.omnibook.broker.domain.repository;

import com.sprint.omnibook.broker.domain.Reservation;
import com.sprint.omnibook.broker.event.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * OTA 플랫폼과 플랫폼 예약 ID로 예약 조회.
     */
    Optional<Reservation> findByPlatformTypeAndPlatformReservationId(PlatformType platformType, String platformReservationId);
}
