package com.sprint.omnibook.broker.api.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.sprint.omnibook.broker.api.exception.ErrorCode
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 구조화된 에러 응답.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val code: String,
    val reason: String,
    val details: Map<String, Any>?,
    val timestamp: String,
    val traceId: String?
) {
    companion object {
        private fun now(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

        /**
         * ErrorCode로부터 기본 응답 생성.
         */
        fun of(errorCode: ErrorCode, traceId: String?): ErrorResponse =
            ErrorResponse(
                code = errorCode.name,
                reason = errorCode.defaultReason,
                details = null,
                timestamp = now(),
                traceId = traceId
            )

        /**
         * ErrorCode와 커스텀 reason으로 응답 생성.
         */
        fun of(errorCode: ErrorCode, reason: String, traceId: String?): ErrorResponse =
            ErrorResponse(
                code = errorCode.name,
                reason = reason,
                details = null,
                timestamp = now(),
                traceId = traceId
            )

        /**
         * ErrorCode와 상세 정보로 응답 생성.
         */
        fun of(errorCode: ErrorCode, details: Map<String, Any>, traceId: String?): ErrorResponse =
            ErrorResponse(
                code = errorCode.name,
                reason = errorCode.defaultReason,
                details = details,
                timestamp = now(),
                traceId = traceId
            )

        /**
         * 모든 필드를 지정하여 응답 생성.
         */
        fun of(errorCode: ErrorCode, reason: String, details: Map<String, Any>, traceId: String?): ErrorResponse =
            ErrorResponse(
                code = errorCode.name,
                reason = reason,
                details = details,
                timestamp = now(),
                traceId = traceId
            )
    }
}
