package com.sprint.omnibook.broker.processing;

import com.sprint.omnibook.broker.domain.*;
import com.sprint.omnibook.broker.domain.repository.*;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 예약 이벤트 처리 서비스.
 * ReservationEvent를 받아 실제 예약/취소 처리를 수행한다.
 *
 * 처리 흐름:
 * 1. ReservationEventEntity 저장 (감사용)
 * 2. PlatformProperty 조회
 * 3. PlatformListing 조회 -> Room 확보
 * 4. 이벤트 타입에 따른 분기 처리
 * 5. ReservationEventEntity markProcessed/markFailed
 */
@Service
@RequiredArgsConstructor
public class ReservationProcessingService {

    private final ReservationEventRepository reservationEventRepository;
    private final PlatformPropertyRepository platformPropertyRepository;
    private final PlatformListingRepository platformListingRepository;
    private final InventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 이벤트 처리.
     *
     * @param event 정규화된 예약 이벤트
     * @return 처리 결과
     */
    @Transactional
    public ProcessingResult process(ReservationEvent event) {
        // 1. ReservationEventEntity 항상 먼저 저장
        ReservationEventEntity eventEntity = createEventEntity(event);
        reservationEventRepository.save(eventEntity);

        // 2. PlatformProperty 조회
        Optional<PlatformProperty> platformPropertyOpt = platformPropertyRepository
                .findByPlatformTypeAndPlatformPropertyId(
                        event.getPlatformType(),
                        event.getPropertyId()
                );

        if (platformPropertyOpt.isEmpty()) {
            return handleFailure(eventEntity, FailureReason.UNKNOWN_PROPERTY);
        }

        // 3. PlatformListing 조회 -> Room 확보
        Optional<PlatformListing> platformListingOpt = platformListingRepository
                .findByPlatformTypeAndPlatformRoomId(
                        event.getPlatformType(),
                        event.getRoomId()
                );

        if (platformListingOpt.isEmpty()) {
            return handleFailure(eventEntity, FailureReason.UNKNOWN_ROOM);
        }

        Room room = platformListingOpt.get().getRoom();

        // 4. 이벤트 타입에 따른 분기 처리
        if (event.getEventType() == EventType.BOOKING) {
            return processBooking(event, eventEntity, room);
        } else {
            return processCancellation(event, eventEntity, room);
        }
    }

    /**
     * BOOKING 이벤트 처리.
     */
    private ProcessingResult processBooking(
            ReservationEvent event,
            ReservationEventEntity eventEntity,
            Room room) {

        // 재고 가용 여부 확인 (checkIn ~ checkOut-1)
        List<Inventory> unavailable = inventoryRepository.findUnavailableByRoomAndDateRange(
                room,
                event.getCheckIn(),
                event.getCheckOut(),
                InventoryStatus.AVAILABLE
        );

        if (!unavailable.isEmpty()) {
            return handleFailure(eventEntity, FailureReason.NOT_AVAILABLE);
        }

        // Reservation 생성
        Reservation reservation = Reservation.builder()
                .platformType(event.getPlatformType())
                .platformReservationId(event.getPlatformReservationId())
                .checkIn(event.getCheckIn())
                .checkOut(event.getCheckOut())
                .guestName(event.getGuestName())
                .guestPhone(event.getGuestPhone())
                .guestEmail(event.getGuestEmail())
                .totalAmount(event.getTotalAmount())
                .status(ReservationStatus.CONFIRMED)
                .bookedAt(event.getOccurredAt())
                .build();

        reservation.setRoom(room);
        reservationRepository.save(reservation);

        // Inventory 예약 처리 (checkIn ~ checkOut-1)
        bookInventory(room, event.getCheckIn(), event.getCheckOut(), reservation);

        // 처리 완료
        return handleSuccess(eventEntity, room, reservation);
    }

    /**
     * CANCELLATION 이벤트 처리.
     */
    private ProcessingResult processCancellation(
            ReservationEvent event,
            ReservationEventEntity eventEntity,
            Room room) {

        // 기존 Reservation 조회
        Optional<Reservation> reservationOpt = reservationRepository
                .findByPlatformTypeAndPlatformReservationId(
                        event.getPlatformType(),
                        event.getPlatformReservationId()
                );

        if (reservationOpt.isEmpty()) {
            // Silent Success: 예약이 없어도 에러 없이 성공 처리
            return handleSuccess(eventEntity, room, null);
        }

        Reservation reservation = reservationOpt.get();

        // Reservation 취소
        reservation.cancel();

        // Inventory 해제 (checkIn ~ checkOut-1)
        releaseInventory(room, reservation.getCheckIn(), reservation.getCheckOut());

        return handleSuccess(eventEntity, room, reservation);
    }

    /**
     * 지정 기간의 Inventory를 예약 처리.
     * Inventory가 없으면 새로 생성한다.
     */
    private void bookInventory(
            Room room,
            LocalDate checkIn,
            LocalDate checkOut,
            Reservation reservation) {

        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            final LocalDate date = current;
            Inventory inventory = inventoryRepository
                    .findByRoomAndDate(room, date)
                    .orElseGet(() -> Inventory.builder()
                            .room(room)
                            .date(date)
                            .build());

            inventory.book(reservation);
            inventoryRepository.save(inventory);

            current = current.plusDays(1);
        }
    }

    /**
     * 지정 기간의 Inventory를 해제 처리.
     */
    private void releaseInventory(Room room, LocalDate checkIn, LocalDate checkOut) {
        // checkOut - 1일까지의 범위 조회
        List<Inventory> inventories = inventoryRepository.findByRoomAndDateBetween(
                room,
                checkIn,
                checkOut.minusDays(1)
        );

        for (Inventory inventory : inventories) {
            inventory.release();
        }
    }

    /**
     * ReservationEventEntity 생성.
     */
    private ReservationEventEntity createEventEntity(ReservationEvent event) {
        return ReservationEventEntity.builder()
                .eventId(event.getEventId())
                .platformType(event.getPlatformType())
                .platformReservationId(event.getPlatformReservationId())
                .eventType(event.getEventType())
                .checkIn(event.getCheckIn())
                .checkOut(event.getCheckOut())
                .guestName(event.getGuestName())
                .guestPhone(event.getGuestPhone())
                .guestEmail(event.getGuestEmail())
                .totalAmount(event.getTotalAmount())
                .propertyName(event.getPropertyName())
                .propertyAddress(event.getPropertyAddress())
                .occurredAt(event.getOccurredAt())
                .receivedAt(event.getReceivedAt())
                .build();
    }

    /**
     * 처리 성공 핸들링.
     */
    private ProcessingResult handleSuccess(
            ReservationEventEntity eventEntity,
            Room room,
            Reservation reservation) {

        eventEntity.markProcessed(room, reservation);
        reservationEventRepository.save(eventEntity);

        return ProcessingResult.success(room, reservation);
    }

    /**
     * 처리 실패 핸들링.
     */
    private ProcessingResult handleFailure(
            ReservationEventEntity eventEntity,
            FailureReason reason) {

        eventEntity.markFailed(reason.name());
        reservationEventRepository.save(eventEntity);

        return ProcessingResult.failure(reason);
    }
}
