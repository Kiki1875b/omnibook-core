package com.sprint.omnibook.broker.ingestion;

/**
 * HTTP 헤더 정보 캡슐화.
 */
public record EventHeaders(
        String eventId,
        String platform,
        String eventType,
        String correlationId
) {
}
