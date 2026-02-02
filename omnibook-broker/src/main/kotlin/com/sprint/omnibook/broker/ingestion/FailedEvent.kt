package com.sprint.omnibook.broker.ingestion

import java.time.Instant

/**
 * 변환 실패한 이벤트 원본 저장용.
 * 추후 재처리를 위해 원본 정보를 보존한다.
 */
data class FailedEvent(
    val eventId: String,
    val platform: String,
    val eventType: String,
    val correlationId: String,
    val reservationId: String,
    val rawPayload: String,
    val errorMessage: String,
    val failedAt: Instant
)
