package com.sprint.omnibook.broker.api.exception

import com.sprint.omnibook.broker.api.dto.ErrorResponse
import com.sprint.omnibook.broker.config.CorrelationIdFilter
import com.sprint.omnibook.broker.translator.TranslationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.util.UUID

/**
 * 전역 예외 처리기.
 * 모든 예외를 구조화된 ErrorResponse로 변환한다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * BrokerException 처리 (도메인 예외).
     */
    @ExceptionHandler(BrokerException::class)
    fun handleBrokerException(ex: BrokerException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("BrokerException: code={}, reason={}, details={}",
            ex.errorCode, ex.message, ex.details)

        val response = ErrorResponse.of(
            ex.errorCode,
            ex.message ?: "",
            ex.details ?: emptyMap(),
            traceId
        )

        return ResponseEntity
            .status(ex.errorCode.httpStatus)
            .body(response)
    }

    /**
     * TranslationException 처리 (payload 변환 실패).
     */
    @ExceptionHandler(TranslationException::class)
    fun handleTranslationException(ex: TranslationException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("TranslationException: {}", ex.message)

        val response = ErrorResponse.of(
            ErrorCode.EVENT_PARSE_ERROR,
            ex.message ?: "",
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.EVENT_PARSE_ERROR.httpStatus)
            .body(response)
    }

    /**
     * 필수 헤더 누락.
     */
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("MissingRequestHeaderException: {}", ex.headerName)

        val response = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            "필수 헤더가 누락되었습니다: ${ex.headerName}",
            mapOf("header" to ex.headerName),
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.httpStatus)
            .body(response)
    }

    /**
     * Request body 누락 또는 JSON 파싱 실패.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("HttpMessageNotReadableException: {}", ex.message)

        val reason = if (ex.message?.contains("Required request body is missing") == true) {
            "Request body가 누락되었습니다."
        } else {
            "Request body를 읽을 수 없습니다."
        }

        val response = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            reason,
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.httpStatus)
            .body(response)
    }

    /**
     * 필수 파라미터 누락.
     */
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParameter(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("MissingServletRequestParameterException: {}", ex.parameterName)

        val response = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            "필수 파라미터가 누락되었습니다: ${ex.parameterName}",
            mapOf("parameter" to ex.parameterName),
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.httpStatus)
            .body(response)
    }

    /**
     * 파라미터 타입 불일치.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("MethodArgumentTypeMismatchException: {} (expected: {})",
            ex.name, ex.requiredType)

        val response = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            "파라미터 타입이 올바르지 않습니다: ${ex.name}",
            mapOf(
                "parameter" to (ex.name ?: "unknown"),
                "expectedType" to (ex.requiredType?.simpleName ?: "unknown")
            ),
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.httpStatus)
            .body(response)
    }

    /**
     * IllegalArgumentException 처리.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.warn("IllegalArgumentException: {}", ex.message)

        val response = ErrorResponse.of(
            ErrorCode.VALIDATION_ERROR,
            ex.message ?: "",
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.httpStatus)
            .body(response)
    }

    /**
     * 기타 모든 예외 처리.
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        val traceId = getTraceId()

        log.error("Unhandled exception: {}", ex.message, ex)

        val response = ErrorResponse.of(
            ErrorCode.INTERNAL_ERROR,
            traceId
        )

        return ResponseEntity
            .status(ErrorCode.INTERNAL_ERROR.httpStatus)
            .body(response)
    }

    /**
     * MDC에서 correlationId를 가져온다.
     * Filter에서 이미 설정되어 있으므로 항상 존재해야 하지만, 안전을 위해 fallback 포함.
     */
    private fun getTraceId(): String {
        val correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)
        return if (!correlationId.isNullOrBlank()) {
            correlationId
        } else {
            UUID.randomUUID().toString().substring(0, 8)
        }
    }
}
