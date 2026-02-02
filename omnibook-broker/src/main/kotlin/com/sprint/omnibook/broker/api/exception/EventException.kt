package com.sprint.omnibook.broker.api.exception

/**
 * 이벤트 관련 예외.
 */
class EventException : BrokerException {

    constructor(errorCode: ErrorCode) : super(errorCode)
    constructor(errorCode: ErrorCode, reason: String) : super(errorCode, reason)
    constructor(errorCode: ErrorCode, details: Map<String, Any>) : super(errorCode, details)

    companion object {
        fun notFound(eventId: String): EventException =
            EventException(ErrorCode.EVENT_NOT_FOUND, mapOf("eventId" to eventId))

        fun parseError(reason: String): EventException =
            EventException(ErrorCode.EVENT_PARSE_ERROR, reason)

        fun invalidPlatform(platform: String): EventException =
            EventException(ErrorCode.INVALID_PLATFORM, mapOf("platform" to platform))

        fun invalidEventType(eventType: String): EventException =
            EventException(ErrorCode.INVALID_EVENT_TYPE, mapOf("eventType" to eventType))
    }
}
