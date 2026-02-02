package com.sprint.omnibook.broker.ingestion

import com.fasterxml.jackson.databind.JsonNode

/**
 * 이벤트 수신 요청 정보.
 * HTTP 헤더와 바디 정보를 통합하여 전달.
 */
data class IngestRequest(
    val eventId: String,
    val platformHeader: String,
    val eventTypeHeader: String,
    val correlationId: String,
    val reservationId: String,
    val payload: JsonNode
)
