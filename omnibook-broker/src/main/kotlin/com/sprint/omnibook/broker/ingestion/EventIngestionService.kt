package com.sprint.omnibook.broker.ingestion

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest
import com.sprint.omnibook.broker.api.exception.ErrorCode
import com.sprint.omnibook.broker.event.EventType
import com.sprint.omnibook.broker.event.PlatformType
import com.sprint.omnibook.broker.persistence.RawEventService
import com.sprint.omnibook.broker.processing.FailureReason
import com.sprint.omnibook.broker.processing.ReservationProcessingService
import com.sprint.omnibook.broker.translator.PayloadTranslator
import com.sprint.omnibook.broker.translator.TranslationException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * 이벤트 수신 및 변환 서비스.
 *
 * 처리 흐름:
 * 1. MongoDB 저장 (RawEventService) - 파싱 없이 즉시 저장
 * 2. 파싱 및 IngestRequest 생성
 * 3. ReservationEvent 생성 (Translator)
 * 4. ReservationProcessingService 호출 (예약/취소 처리)
 */
@Service
class EventIngestionService(
    private val rawEventService: RawEventService,
    private val translators: Map<PlatformType, PayloadTranslator>,
    private val failedEventStore: FailedEventStore,
    private val objectMapper: ObjectMapper,
    private val reservationProcessingService: ReservationProcessingService
) {

    /**
     * 이벤트 처리 진입점.
     * MongoDB 저장 -> 파싱 -> Translator 처리 -> 예약 처리 순서를 보장한다.
     *
     * @param rawBody HTTP body 원본
     * @param headers HTTP 헤더 정보
     * @return 처리 결과
     */
    fun process(rawBody: String, headers: EventHeaders): IngestionResult {
        // 1. 즉시 MongoDB 저장 (파싱 실패와 무관하게 원본 보존)
        rawEventService.store(rawBody, headers)

        // 2. 파싱 및 IngestRequest 생성
        val request: IngestRequest
        try {
            request = parseToIngestRequest(rawBody, headers)
        } catch (e: JsonProcessingException) {
            val eventId = resolveEventId(headers.eventId, null)
            val reason = IngestionErrorMessage.JSON_PARSE_FAILED_PREFIX + e.message
            saveFailedEventForParseError(eventId, headers, rawBody, e.message ?: "")
            return IngestionResult.failure(eventId, reason, ErrorCode.EVENT_PARSE_ERROR)
        }

        // 3. 비즈니스 처리
        return ingest(request)
    }

    private fun parseToIngestRequest(rawBody: String, headers: EventHeaders): IngestRequest {
        val request = objectMapper.readValue(rawBody, IncomingEventRequest::class.java)
        val eventId = resolveEventId(headers.eventId, request.eventId)

        return IngestRequest(
            eventId = eventId,
            platformHeader = headers.platform,
            eventTypeHeader = headers.eventType,
            correlationId = headers.correlationId,
            reservationId = request.reservationId ?: "",
            payload = request.payload!!
        )
    }

    private fun resolveEventId(headerEventId: String?, bodyEventId: String?): String {
        if (!headerEventId.isNullOrBlank()) {
            return headerEventId
        }
        if (!bodyEventId.isNullOrBlank()) {
            return bodyEventId
        }
        return UUID.randomUUID().toString()
    }

    private fun saveFailedEventForParseError(eventId: String, headers: EventHeaders, rawBody: String, errorMessage: String) {
        val failed = FailedEvent(
            eventId = eventId,
            platform = headers.platform,
            eventType = headers.eventType,
            correlationId = headers.correlationId,
            reservationId = "",
            rawPayload = rawBody,
            errorMessage = IngestionErrorMessage.JSON_PARSE_FAILED_PREFIX + errorMessage,
            failedAt = Instant.now()
        )

        failedEventStore.save(failed)
    }

    /**
     * Translator 처리 및 예약 처리.
     *
     * @return 처리 결과 (성공/실패 및 실패 사유 포함)
     */
    internal fun ingest(request: IngestRequest): IngestionResult {
        val platform = mapPlatform(request.platformHeader)
        val eventType = mapEventType(request.eventTypeHeader)

        if (platform == null) {
            val reason = IngestionErrorMessage.UNKNOWN_PLATFORM_PREFIX + request.platformHeader
            saveFailedEvent(request, reason)
            return IngestionResult.failure(request.eventId, reason, ErrorCode.INVALID_PLATFORM)
        }

        val translator = translators[platform]
        if (translator == null) {
            val reason = IngestionErrorMessage.TRANSLATOR_NOT_FOUND_PREFIX + platform
            saveFailedEvent(request, reason)
            return IngestionResult.failure(request.eventId, reason, ErrorCode.TRANSLATOR_NOT_FOUND)
        }

        val rawPayload = extractRawPayload(request)
        if (rawPayload == null) {
            saveFailedEvent(request, IngestionErrorMessage.PAYLOAD_SERIALIZATION_FAILED)
            return IngestionResult.failure(request.eventId, IngestionErrorMessage.PAYLOAD_SERIALIZATION_FAILED, ErrorCode.PAYLOAD_SERIALIZATION_FAILED)
        }

        return try {
            // 1단계: Translator로 정규화된 이벤트 생성
            val event = translator.translate(rawPayload, eventType)

            // 2단계: 예약 처리 서비스 호출
            val result = reservationProcessingService.process(event)

            if (!result.success) {
                // ReservationProcessingService 내부에서 이미 실패 처리됨
                // FailedEventStore에는 별도 저장하지 않음 (ReservationEventEntity에 기록됨)
                val failureReason = result.failureReason
                val reason = failureReason?.name ?: IngestionErrorMessage.PROCESSING_FAILED
                val errorCode = mapFailureReasonToErrorCode(failureReason)
                IngestionResult.failure(request.eventId, reason, errorCode)
            } else {
                IngestionResult.success(request.eventId)
            }
        } catch (e: TranslationException) {
            val reason = e.message ?: ""
            saveFailedEvent(request, reason)
            IngestionResult.failure(request.eventId, reason, ErrorCode.EVENT_PARSE_ERROR)
        }
    }

    /**
     * 헤더 문자열을 PlatformType으로 변환한다.
     */
    private fun mapPlatform(header: String?): PlatformType? {
        if (header == null) return null
        return when (header.uppercase()) {
            PlatformHeaderAlias.YANOLJA_SHORT, PlatformHeaderAlias.YANOLJA -> PlatformType.YANOLJA
            PlatformHeaderAlias.AIRBNB_SHORT, PlatformHeaderAlias.AIRBNB -> PlatformType.AIRBNB
            PlatformHeaderAlias.YEOGIEOTTAE_SHORT, PlatformHeaderAlias.YEOGIEOTTAE -> PlatformType.YEOGIEOTTAE
            else -> null
        }
    }

    /**
     * 헤더 문자열을 EventType으로 변환한다.
     */
    private fun mapEventType(header: String?): EventType {
        if (header == null) return EventType.BOOKING
        return when (header.uppercase()) {
            EventTypeHeaderAlias.CANCEL, EventTypeHeaderAlias.CANCELLATION -> EventType.CANCELLATION
            else -> EventType.BOOKING
        }
    }

    private fun extractRawPayload(request: IngestRequest): String? {
        return try {
            objectMapper.writeValueAsString(request.payload)
        } catch (e: JsonProcessingException) {
            null
        }
    }

    private fun saveFailedEvent(request: IngestRequest, errorMessage: String) {
        val rawPayload = try {
            objectMapper.writeValueAsString(request.payload)
        } catch (e: JsonProcessingException) {
            IngestionErrorMessage.SERIALIZATION_FAILED
        }

        val failed = FailedEvent(
            eventId = request.eventId,
            platform = request.platformHeader,
            eventType = request.eventTypeHeader,
            correlationId = request.correlationId,
            reservationId = request.reservationId,
            rawPayload = rawPayload,
            errorMessage = errorMessage,
            failedAt = Instant.now()
        )

        failedEventStore.save(failed)
    }

    /**
     * FailureReason을 ErrorCode로 매핑한다.
     */
    private fun mapFailureReasonToErrorCode(failureReason: FailureReason?): ErrorCode {
        if (failureReason == null) {
            return ErrorCode.PROCESSING_FAILED
        }
        return when (failureReason) {
            FailureReason.UNKNOWN_ROOM -> ErrorCode.UNKNOWN_ROOM
            FailureReason.NOT_AVAILABLE -> ErrorCode.NOT_AVAILABLE
            FailureReason.ROOM_ALREADY_BOOKED -> ErrorCode.ROOM_ALREADY_BOOKED
        }
    }
}
