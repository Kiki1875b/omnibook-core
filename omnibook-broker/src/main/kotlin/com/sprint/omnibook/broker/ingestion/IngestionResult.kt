package com.sprint.omnibook.broker.ingestion

import com.sprint.omnibook.broker.api.exception.ErrorCode

/**
 * 이벤트 처리 결과.
 */
data class IngestionResult(
    val eventId: String,
    val success: Boolean,
    val failureReason: String?,
    val errorCode: ErrorCode?
) {
    companion object {
        /**
         * 성공 결과 생성.
         */
        fun success(eventId: String): IngestionResult =
            IngestionResult(eventId, true, null, null)

        /**
         * 실패 결과 생성.
         */
        fun failure(eventId: String, reason: String, errorCode: ErrorCode): IngestionResult =
            IngestionResult(eventId, false, reason, errorCode)
    }
}
