package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest;
import com.sprint.omnibook.broker.api.exception.ErrorCode;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.persistence.RawEventService;
import com.sprint.omnibook.broker.processing.FailureReason;
import com.sprint.omnibook.broker.processing.ProcessingResult;
import com.sprint.omnibook.broker.processing.ReservationProcessingService;
import com.sprint.omnibook.broker.translator.PayloadTranslator;
import com.sprint.omnibook.broker.translator.TranslationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
@RequiredArgsConstructor
public class EventIngestionService {

    private final RawEventService rawEventService;
    private final Map<PlatformType, PayloadTranslator> translators;
    private final FailedEventStore failedEventStore;
    private final ObjectMapper objectMapper;
    private final ReservationProcessingService reservationProcessingService;

    /**
     * 이벤트 처리 진입점.
     * MongoDB 저장 -> 파싱 -> Translator 처리 -> 예약 처리 순서를 보장한다.
     *
     * @param rawBody HTTP body 원본
     * @param headers HTTP 헤더 정보
     * @return 처리 결과
     */
    public IngestionResult process(String rawBody, EventHeaders headers) {
        // 1. 즉시 MongoDB 저장 (파싱 실패와 무관하게 원본 보존)
        rawEventService.store(rawBody, headers);

        // 2. 파싱 및 IngestRequest 생성
        IngestRequest request;
        try {
            request = parseToIngestRequest(rawBody, headers);
        } catch (JsonProcessingException e) {
            String eventId = resolveEventId(headers.eventId(), null);
            String reason = IngestionErrorMessage.JSON_PARSE_FAILED_PREFIX + e.getMessage();
            saveFailedEventForParseError(eventId, headers, rawBody, e.getMessage());
            return IngestionResult.failure(eventId, reason, ErrorCode.EVENT_PARSE_ERROR);
        }

        // 3. 비즈니스 처리
        return ingest(request);
    }

    private IngestRequest parseToIngestRequest(String rawBody, EventHeaders headers) throws JsonProcessingException {
        IncomingEventRequest request = objectMapper.readValue(rawBody, IncomingEventRequest.class);
        String eventId = resolveEventId(headers.eventId(), request.getEventId());

        return new IngestRequest(
                eventId,
                headers.platform(),
                headers.eventType(),
                headers.correlationId(),
                request.getReservationId(),
                request.getPayload()
        );
    }

    private String resolveEventId(String headerEventId, String bodyEventId) {
        if (headerEventId != null && !headerEventId.isBlank()) {
            return headerEventId;
        }
        if (bodyEventId != null && !bodyEventId.isBlank()) {
            return bodyEventId;
        }
        return UUID.randomUUID().toString();
    }

    private void saveFailedEventForParseError(String eventId, EventHeaders headers, String rawBody, String errorMessage) {
        FailedEvent failed = FailedEvent.builder()
                .eventId(eventId)
                .platform(headers.platform())
                .eventType(headers.eventType())
                .correlationId(headers.correlationId())
                .rawPayload(rawBody)
                .errorMessage(IngestionErrorMessage.JSON_PARSE_FAILED_PREFIX + errorMessage)
                .failedAt(Instant.now())
                .build();

        failedEventStore.save(failed);
    }

    /**
     * Translator 처리 및 예약 처리.
     *
     * @return 처리 결과 (성공/실패 및 실패 사유 포함)
     */
    IngestionResult ingest(IngestRequest request) {
        PlatformType platform = mapPlatform(request.platformHeader());
        EventType eventType = mapEventType(request.eventTypeHeader());

        if (platform == null) {
            String reason = IngestionErrorMessage.UNKNOWN_PLATFORM_PREFIX + request.platformHeader();
            saveFailedEvent(request, reason);
            return IngestionResult.failure(request.eventId(), reason, ErrorCode.INVALID_PLATFORM);
        }

        PayloadTranslator translator = translators.get(platform);
        if (translator == null) {
            String reason = IngestionErrorMessage.TRANSLATOR_NOT_FOUND_PREFIX + platform;
            saveFailedEvent(request, reason);
            return IngestionResult.failure(request.eventId(), reason, ErrorCode.TRANSLATOR_NOT_FOUND);
        }

        String rawPayload = extractRawPayload(request);
        if (rawPayload == null) {
            saveFailedEvent(request, IngestionErrorMessage.PAYLOAD_SERIALIZATION_FAILED);
            return IngestionResult.failure(request.eventId(), IngestionErrorMessage.PAYLOAD_SERIALIZATION_FAILED, ErrorCode.PAYLOAD_SERIALIZATION_FAILED);
        }

        try {
            // 1단계: Translator로 정규화된 이벤트 생성
            ReservationEvent event = translator.translate(rawPayload, eventType);

            // 2단계: 예약 처리 서비스 호출
            ProcessingResult result = reservationProcessingService.process(event);

            if (!result.isSuccess()) {
                // ReservationProcessingService 내부에서 이미 실패 처리됨
                // FailedEventStore에는 별도 저장하지 않음 (ReservationEventEntity에 기록됨)
                FailureReason failureReason = result.getFailureReason();
                String reason = failureReason != null
                        ? failureReason.name()
                        : IngestionErrorMessage.PROCESSING_FAILED;
                ErrorCode errorCode = mapFailureReasonToErrorCode(failureReason);
                return IngestionResult.failure(request.eventId(), reason, errorCode);
            }

            return IngestionResult.success(request.eventId());
        } catch (TranslationException e) {
            String reason = e.getMessage();
            saveFailedEvent(request, reason);
            return IngestionResult.failure(request.eventId(), reason, ErrorCode.EVENT_PARSE_ERROR);
        }
    }

    /**
     * 헤더 문자열을 PlatformType으로 변환한다.
     */
    private PlatformType mapPlatform(String header) {
        if (header == null) return null;
        return switch (header.toUpperCase()) {
            case PlatformHeaderAlias.YANOLJA_SHORT, PlatformHeaderAlias.YANOLJA -> PlatformType.YANOLJA;
            case PlatformHeaderAlias.AIRBNB_SHORT, PlatformHeaderAlias.AIRBNB -> PlatformType.AIRBNB;
            case PlatformHeaderAlias.YEOGIEOTTAE_SHORT, PlatformHeaderAlias.YEOGIEOTTAE -> PlatformType.YEOGIEOTTAE;
            default -> null;
        };
    }

    /**
     * 헤더 문자열을 EventType으로 변환한다.
     */
    private EventType mapEventType(String header) {
        if (header == null) return EventType.BOOKING;
        return switch (header.toUpperCase()) {
            case EventTypeHeaderAlias.CANCEL, EventTypeHeaderAlias.CANCELLATION -> EventType.CANCELLATION;
            default -> EventType.BOOKING;
        };
    }

    private String extractRawPayload(IngestRequest request) {
        try {
            return objectMapper.writeValueAsString(request.payload());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void saveFailedEvent(IngestRequest request, String errorMessage) {
        String rawPayload;
        try {
            rawPayload = objectMapper.writeValueAsString(request.payload());
        } catch (JsonProcessingException e) {
            rawPayload = IngestionErrorMessage.SERIALIZATION_FAILED;
        }

        FailedEvent failed = FailedEvent.builder()
                .eventId(request.eventId())
                .platform(request.platformHeader())
                .eventType(request.eventTypeHeader())
                .correlationId(request.correlationId())
                .reservationId(request.reservationId())
                .rawPayload(rawPayload)
                .errorMessage(errorMessage)
                .failedAt(Instant.now())
                .build();

        failedEventStore.save(failed);
    }

    /**
     * FailureReason을 ErrorCode로 매핑한다.
     */
    private ErrorCode mapFailureReasonToErrorCode(FailureReason failureReason) {
        if (failureReason == null) {
            return ErrorCode.PROCESSING_FAILED;
        }
        return switch (failureReason) {
            case UNKNOWN_ROOM -> ErrorCode.UNKNOWN_ROOM;
            case NOT_AVAILABLE -> ErrorCode.NOT_AVAILABLE;
            case ROOM_ALREADY_BOOKED -> ErrorCode.ROOM_ALREADY_BOOKED;
        };
    }
}
