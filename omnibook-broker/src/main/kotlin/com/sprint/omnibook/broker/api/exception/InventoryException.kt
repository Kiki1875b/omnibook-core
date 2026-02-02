package com.sprint.omnibook.broker.api.exception

import com.sprint.omnibook.broker.processing.FailureReason
import java.time.LocalDate

/**
 * 재고/객실 관련 예외.
 * FailureReason과 매핑되어 처리 실패를 예외로 변환한다.
 */
class InventoryException : BrokerException {

    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, reason: String) : super(errorCode, reason)
    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode, details)

    companion object {
        /**
         * FailureReason을 InventoryException으로 변환.
         */
        fun fromFailureReason(reason: FailureReason): InventoryException =
            when (reason) {
                FailureReason.UNKNOWN_ROOM -> InventoryException(ErrorCode.UNKNOWN_ROOM)
                FailureReason.NOT_AVAILABLE -> InventoryException(ErrorCode.NOT_AVAILABLE)
                FailureReason.ROOM_ALREADY_BOOKED -> InventoryException(ErrorCode.ROOM_ALREADY_BOOKED)
            }

        /**
         * FailureReason을 상세 정보와 함께 InventoryException으로 변환.
         */
        fun fromFailureReason(reason: FailureReason, details: Map<String, Any>): InventoryException =
            when (reason) {
                FailureReason.UNKNOWN_ROOM -> InventoryException(ErrorCode.UNKNOWN_ROOM, details)
                FailureReason.NOT_AVAILABLE -> InventoryException(ErrorCode.NOT_AVAILABLE, details)
                FailureReason.ROOM_ALREADY_BOOKED -> InventoryException(ErrorCode.ROOM_ALREADY_BOOKED, details)
            }

        fun unknownRoom(platformRoomId: String): InventoryException =
            InventoryException(ErrorCode.UNKNOWN_ROOM, mapOf("platformRoomId" to platformRoomId))

        fun notAvailable(roomId: String, date: LocalDate): InventoryException =
            InventoryException(ErrorCode.NOT_AVAILABLE, mapOf("roomId" to roomId, "date" to date.toString()))

        fun roomAlreadyBooked(roomId: String, checkIn: LocalDate, checkOut: LocalDate): InventoryException =
            InventoryException(
                ErrorCode.ROOM_ALREADY_BOOKED,
                mapOf("roomId" to roomId, "checkIn" to checkIn.toString(), "checkOut" to checkOut.toString())
            )

        fun roomNotFound(roomId: String): InventoryException =
            InventoryException(ErrorCode.ROOM_NOT_FOUND, mapOf("roomId" to roomId))

        fun invalidDateRange(startDate: LocalDate, endDate: LocalDate): InventoryException =
            InventoryException(
                ErrorCode.INVALID_DATE_RANGE,
                mapOf("startDate" to startDate.toString(), "endDate" to endDate.toString())
            )
    }
}
