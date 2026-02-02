package com.sprint.omnibook.broker.api.exception

/**
 * 예약 관련 예외.
 */
class ReservationException : BrokerException {

    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, reason: String) : super(errorCode, reason)
    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode, details)

    companion object {
        fun notFound(reservationId: String): ReservationException =
            ReservationException(ErrorCode.RESERVATION_NOT_FOUND, mapOf("reservationId" to reservationId))

        fun duplicate(reservationId: String): ReservationException =
            ReservationException(ErrorCode.DUPLICATE_RESERVATION, mapOf("reservationId" to reservationId))

        fun alreadyCancelled(reservationId: String): ReservationException =
            ReservationException(ErrorCode.RESERVATION_ALREADY_CANCELLED, mapOf("reservationId" to reservationId))
    }
}
