package com.sprint.omnibook.broker.api.dto

import com.fasterxml.jackson.databind.JsonNode

/**
 * 시뮬레이터로부터 수신하는 이벤트 요청 바디.
 *
 * Body 형식:
 * {
 *   "eventId": "...",
 *   "reservationId": "...",
 *   "payload": { OTA specific JSON }
 * }
 */
data class IncomingEventRequest(
    var eventId: String? = null,
    var reservationId: String? = null,
    var payload: JsonNode? = null
)
