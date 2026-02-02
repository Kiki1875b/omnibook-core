package com.sprint.omnibook.broker.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sprint.omnibook.broker.api.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 구조화된 에러 응답.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String reason;
    private final Map<String, Object> details;
    private final String timestamp;
    private final String traceId;

    private static String now() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    /**
     * ErrorCode로부터 기본 응답 생성.
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .reason(errorCode.getDefaultReason())
                .timestamp(now())
                .traceId(traceId)
                .build();
    }

    /**
     * ErrorCode와 커스텀 reason으로 응답 생성.
     */
    public static ErrorResponse of(ErrorCode errorCode, String reason, String traceId) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .reason(reason)
                .timestamp(now())
                .traceId(traceId)
                .build();
    }

    /**
     * ErrorCode와 상세 정보로 응답 생성.
     */
    public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details, String traceId) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .reason(errorCode.getDefaultReason())
                .details(details)
                .timestamp(now())
                .traceId(traceId)
                .build();
    }

    /**
     * 모든 필드를 지정하여 응답 생성.
     */
    public static ErrorResponse of(ErrorCode errorCode, String reason, Map<String, Object> details, String traceId) {
        return ErrorResponse.builder()
                .code(errorCode.name())
                .reason(reason)
                .details(details)
                .timestamp(now())
                .traceId(traceId)
                .build();
    }
}
