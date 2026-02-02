package com.sprint.omnibook.broker.api.exception;

import com.sprint.omnibook.broker.api.dto.ErrorResponse;
import com.sprint.omnibook.broker.config.CorrelationIdFilter;
import com.sprint.omnibook.broker.translator.TranslationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

/**
 * 전역 예외 처리기.
 * 모든 예외를 구조화된 ErrorResponse로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BrokerException 처리 (도메인 예외).
     */
    @ExceptionHandler(BrokerException.class)
    public ResponseEntity<ErrorResponse> handleBrokerException(BrokerException ex) {
        String traceId = getTraceId();

        log.warn("BrokerException: code={}, reason={}, details={}",
                ex.getErrorCode(), ex.getMessage(), ex.getDetails());

        ErrorResponse response = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getDetails(),
                traceId
        );

        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(response);
    }

    /**
     * TranslationException 처리 (payload 변환 실패).
     */
    @ExceptionHandler(TranslationException.class)
    public ResponseEntity<ErrorResponse> handleTranslationException(TranslationException ex) {
        String traceId = getTraceId();

        log.warn("TranslationException: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.EVENT_PARSE_ERROR,
                ex.getMessage(),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.EVENT_PARSE_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * 필수 헤더 누락.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        String traceId = getTraceId();

        log.warn("MissingRequestHeaderException: {}", ex.getHeaderName());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                "필수 헤더가 누락되었습니다: " + ex.getHeaderName(),
                Map.of("header", ex.getHeaderName()),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * Request body 누락 또는 JSON 파싱 실패.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String traceId = getTraceId();

        log.warn("HttpMessageNotReadableException: {}", ex.getMessage());

        String reason = ex.getMessage() != null && ex.getMessage().contains("Required request body is missing")
                ? "Request body가 누락되었습니다."
                : "Request body를 읽을 수 없습니다.";

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                reason,
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * 필수 파라미터 누락.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String traceId = getTraceId();

        log.warn("MissingServletRequestParameterException: {}", ex.getParameterName());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                "필수 파라미터가 누락되었습니다: " + ex.getParameterName(),
                Map.of("parameter", ex.getParameterName()),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * 파라미터 타입 불일치.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String traceId = getTraceId();

        log.warn("MethodArgumentTypeMismatchException: {} (expected: {})",
                ex.getName(), ex.getRequiredType());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                "파라미터 타입이 올바르지 않습니다: " + ex.getName(),
                Map.of(
                        "parameter", ex.getName(),
                        "expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
                ),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * IllegalArgumentException 처리.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = getTraceId();

        log.warn("IllegalArgumentException: {}", ex.getMessage());

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.VALIDATION_ERROR,
                ex.getMessage(),
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * 기타 모든 예외 처리.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        String traceId = getTraceId();

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                traceId
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(response);
    }

    /**
     * MDC에서 correlationId를 가져온다.
     * Filter에서 이미 설정되어 있으므로 항상 존재해야 하지만, 안전을 위해 fallback 포함.
     */
    private String getTraceId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
