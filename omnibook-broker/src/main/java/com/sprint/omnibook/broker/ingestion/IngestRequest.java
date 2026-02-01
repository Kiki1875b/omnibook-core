package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 이벤트 수신 요청 정보.
 * HTTP 헤더와 바디 정보를 통합하여 전달.
 */
public record IngestRequest(
        String eventId,
        String platformHeader,
        String eventTypeHeader,
        String correlationId,
        String reservationId,
        JsonNode payload
) {
}
