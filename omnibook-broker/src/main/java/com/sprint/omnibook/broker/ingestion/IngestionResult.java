package com.sprint.omnibook.broker.ingestion;

import com.sprint.omnibook.broker.api.exception.ErrorCode;

/**
 * 이벤트 처리 결과.
 */
public record IngestionResult(
        String eventId,
        boolean success,
        String failureReason,
        ErrorCode errorCode
) {
    /**
     * 성공 결과 생성.
     */
    public static IngestionResult success(String eventId) {
        return new IngestionResult(eventId, true, null, null);
    }

    /**
     * 실패 결과 생성.
     */
    public static IngestionResult failure(String eventId, String reason, ErrorCode errorCode) {
        return new IngestionResult(eventId, false, reason, errorCode);
    }
}
