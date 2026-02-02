package com.sprint.omnibook.broker.processing

import com.sprint.omnibook.broker.domain.*
import com.sprint.omnibook.broker.domain.repository.*
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.ReservationEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 예약 이벤트 처리 서비스.
 * ReservationEvent를 받아 실제 예약/취소 처리를 수행한다.
 *
 * 처리 흐름:
 * 1. ReservationEventEntity 저장 (감사용)
 * 2. PlatformListing 조회 -> Room 확보
 * 3. 이벤트 타입에 따른 분기 처리
 * 4. ReservationEventEntity markProcessed/markFailed
 */
@Service
class ReservationProcessingService(
    private val reservationEventRepository: ReservationEventRepository,
    private val platformListingRepository: PlatformListingRepository,
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository
) {

    /**
     * 이벤트 처리.
     *
     * @param event 정규화된 예약 이벤트
     * @return 처리 결과
     */
    @Transactional
    fun process(event: ReservationEvent): ProcessingResult {
        // 1. ReservationEventEntity 항상 먼저 저장
        val eventEntity = createEventEntity(event)
        reservationEventRepository.save(eventEntity)

        // 2. PlatformListing 조회 -> Room 확보
        val platformListing = platformListingRepository
            .findByPlatformTypeAndPlatformRoomId(
                event.platformType,
                event.roomId ?: return handleFailure(eventEntity, FailureReason.UNKNOWN_ROOM)
            )

        if (platformListing == null) {
            return handleFailure(eventEntity, FailureReason.UNKNOWN_ROOM)
        }

        val room = platformListing.room!!

        // 3. 이벤트 타입에 따른 분기 처리
        return if (event.eventType == EventType.BOOKING) {
            processBooking(event, eventEntity, room)
        } else {
            processCancellation(event, eventEntity, room)
        }
    }

    /**
     * BOOKING 이벤트 처리.
     */
    private fun processBooking(
        event: ReservationEvent,
        eventEntity: ReservationEventEntity,
        room: Room
    ): ProcessingResult {

        val checkIn = event.checkIn ?: return handleFailure(eventEntity, FailureReason.NOT_AVAILABLE)
        val checkOut = event.checkOut ?: return handleFailure(eventEntity, FailureReason.NOT_AVAILABLE)

        // 재고 가용 여부 확인 (checkIn ~ checkOut-1)
        val unavailable = inventoryRepository.findUnavailableByRoomAndDateRange(
            room,
            checkIn,
            checkOut,
            InventoryStatus.AVAILABLE
        )

        if (unavailable.isNotEmpty()) {
            return handleFailure(eventEntity, FailureReason.NOT_AVAILABLE)
        }

        // Reservation 생성
        val reservation = Reservation.book(room, event)
        reservationRepository.save(reservation)

        // Inventory 예약 처리 (checkIn ~ checkOut-1)
        bookInventory(room, checkIn, checkOut, reservation)

        // 처리 완료
        return handleSuccess(eventEntity, room, reservation)
    }

    /**
     * CANCELLATION 이벤트 처리.
     */
    private fun processCancellation(
        event: ReservationEvent,
        eventEntity: ReservationEventEntity,
        room: Room
    ): ProcessingResult {

        // 기존 Reservation 조회
        val reservation = reservationRepository
            .findByPlatformTypeAndPlatformReservationId(
                event.platformType,
                event.platformReservationId ?: return handleSuccess(eventEntity, room, null)
            )

        if (reservation == null) {
            // Silent Success: 예약이 없어도 에러 없이 성공 처리
            return handleSuccess(eventEntity, room, null)
        }

        // Reservation 취소
        reservation.cancel()

        // Inventory 해제 (checkIn ~ checkOut-1)
        releaseInventory(room, reservation.checkIn!!, reservation.checkOut!!)

        return handleSuccess(eventEntity, room, reservation)
    }

    /**
     * 지정 기간의 Inventory를 예약 처리.
     * Inventory가 없으면 새로 생성한다.
     */
    private fun bookInventory(
        room: Room,
        checkIn: LocalDate,
        checkOut: LocalDate,
        reservation: Reservation
    ) {
        var current = checkIn
        while (current.isBefore(checkOut)) {
            val date = current
            val inventory = inventoryRepository
                .findByRoomAndDate(room, date)
                ?: Inventory.createBooked(room, date)

            inventory.book(reservation)
            inventoryRepository.save(inventory)

            current = current.plusDays(1)
        }
    }

    /**
     * 지정 기간의 Inventory를 해제 처리.
     */
    private fun releaseInventory(room: Room, checkIn: LocalDate, checkOut: LocalDate) {
        // checkOut - 1일까지의 범위 조회
        val inventories = inventoryRepository.findByRoomAndDateBetween(
            room,
            checkIn,
            checkOut.minusDays(1)
        )

        inventories.forEach { it.release() }
    }

    /**
     * ReservationEventEntity 생성.
     */
    private fun createEventEntity(event: ReservationEvent): ReservationEventEntity {
        return ReservationEventEntity(
            eventId = event.eventId,
            platformType = event.platformType,
            platformReservationId = event.platformReservationId ?: "",
            eventType = event.eventType ?: EventType.BOOKING,
            checkIn = event.checkIn,
            checkOut = event.checkOut,
            guestName = event.guestName,
            guestPhone = event.guestPhone,
            guestEmail = event.guestEmail,
            totalAmount = event.totalAmount,
            propertyName = event.propertyName,
            propertyAddress = event.propertyAddress,
            occurredAt = event.occurredAt,
            receivedAt = event.receivedAt,
            status = event.status.name
        )
    }

    /**
     * 처리 성공 핸들링.
     */
    private fun handleSuccess(
        eventEntity: ReservationEventEntity,
        room: Room,
        reservation: Reservation?
    ): ProcessingResult {

        if (reservation != null) {
            eventEntity.markProcessed(room, reservation)
        }
        reservationEventRepository.save(eventEntity)

        return ProcessingResult.success(room, reservation)
    }

    /**
     * 처리 실패 핸들링.
     */
    private fun handleFailure(
        eventEntity: ReservationEventEntity,
        reason: FailureReason
    ): ProcessingResult {

        eventEntity.markFailed(reason.name)
        reservationEventRepository.save(eventEntity)

        return ProcessingResult.failure(reason)
    }
}
