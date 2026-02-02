package com.sprint.omnibook.broker.api.dto

/**
 * 이벤트 수신 응답.
 */
data class EventResponse(
    val eventId: String,
    val status: String,
    val message: String
) {
    companion object {
        private object Status {
            const val ACCEPTED = "ACCEPTED"
            const val FAILED = "FAILED"
            const val SAVED_FOR_RETRY = "SAVED_FOR_RETRY"
        }

        private object Message {
            const val ACCEPTED = "이벤트가 정상 처리되었습니다."
            const val SAVED_FOR_RETRY = "변환 실패. 원본이 저장되었습니다."
        }

        /**
         * 성공 응답을 생성한다.
         */
        fun accepted(eventId: String): EventResponse =
            EventResponse(eventId, Status.ACCEPTED, Message.ACCEPTED)

        /**
         * 실패 응답을 생성한다.
         */
        fun failed(eventId: String, reason: String): EventResponse =
            EventResponse(eventId, Status.FAILED, reason)

        /**
         * 재시도 대기 응답을 생성한다.
         */
        fun savedForRetry(eventId: String): EventResponse =
            EventResponse(eventId, Status.SAVED_FOR_RETRY, Message.SAVED_FOR_RETRY)

        /**
         * 사유를 포함한 재시도 대기 응답을 생성한다.
         */
        fun savedForRetry(eventId: String, reason: String): EventResponse =
            EventResponse(eventId, Status.SAVED_FOR_RETRY, reason)
    }
}
