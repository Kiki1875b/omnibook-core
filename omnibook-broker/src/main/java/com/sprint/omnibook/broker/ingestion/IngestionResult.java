package com.sprint.omnibook.broker.ingestion;

/**
 * 이벤트 처리 결과.
 */
public record IngestionResult(
        String eventId,
        boolean success
) {
}
