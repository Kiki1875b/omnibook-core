package com.sprint.omnibook.broker.ingestion

/**
 * HTTP 헤더 정보 캡슐화.
 */
data class EventHeaders(
    val eventId: String,
    val platform: String,
    val eventType: String,
    val correlationId: String
)
