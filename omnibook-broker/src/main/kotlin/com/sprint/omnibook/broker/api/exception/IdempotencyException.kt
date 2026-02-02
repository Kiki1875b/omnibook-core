package com.sprint.omnibook.broker.api.exception

/**
 * 멱등성 관련 예외.
 */
class IdempotencyException : BrokerException {

    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode, details)

    companion object {
        fun keyNotFound(eventId: String): IdempotencyException =
            IdempotencyException(ErrorCode.IDEMPOTENCY_KEY_NOT_FOUND, mapOf("eventId" to eventId))
    }
}
