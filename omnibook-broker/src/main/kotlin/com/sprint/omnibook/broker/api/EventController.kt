package com.sprint.omnibook.broker.api

import com.sprint.omnibook.broker.api.dto.ErrorResponse
import com.sprint.omnibook.broker.api.dto.EventResponse
import com.sprint.omnibook.broker.ingestion.EventHeaders
import com.sprint.omnibook.broker.ingestion.EventIngestionService
import com.sprint.omnibook.broker.ingestion.EventTypeHeaderAlias
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 외부 이벤트 수신 컨트롤러.
 * HTTP 요청/응답 처리만 담당하는 thin layer.
 */
@RestController
@RequestMapping("/api/events")
class EventController(
    private val ingestionService: EventIngestionService
) {

    companion object {
        private const val MDC_CORRELATION_ID = "correlationId"
    }

    /**
     * 외부 플랫폼으로부터 예약 이벤트를 수신하고 처리 결과를 반환한다.
     */
    @PostMapping
    fun receiveEvent(
        @RequestHeader(value = "X-Event-Id", required = false) eventId: String?,
        @RequestHeader(value = "X-Platform", required = true) platform: String,
        @RequestHeader(value = "X-Event-Type", required = false, defaultValue = EventTypeHeaderAlias.BOOKING) eventType: String,
        @RequestHeader(value = "X-Correlation-Id", required = false) correlationId: String?,
        @RequestBody rawBody: String
    ): ResponseEntity<*> {

        val headers = EventHeaders(
            eventId = eventId ?: "",
            platform = platform,
            eventType = eventType,
            correlationId = correlationId ?: ""
        )
        val result = ingestionService.process(rawBody, headers)

        return if (result.success) {
            ResponseEntity.ok(EventResponse.accepted(result.eventId))
        } else {
            val errorResponse = ErrorResponse.of(
                result.errorCode!!,
                result.failureReason ?: "",
                mapOf("eventId" to result.eventId),
                MDC.get(MDC_CORRELATION_ID)
            )

            ResponseEntity
                .status(result.errorCode.httpStatus)
                .body(errorResponse)
        }
    }
}
