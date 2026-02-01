package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.persistence.RawEventService;
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
            saveFailedEventForParseError(eventId, headers, rawBody, e.getMessage());
            return new IngestionResult(eventId, false);
        }

        // 3. 비즈니스 처리
        boolean success = ingest(request);
        return new IngestionResult(request.eventId(), success);
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
                .errorMessage("JSON 파싱 실패: " + errorMessage)
                .failedAt(Instant.now())
                .build();

        failedEventStore.save(failed);
    }

    /**
     * Translator 처리 및 예약 처리.
     *
     * @return 처리 성공 시 true, 실패 시 false
     */
    boolean ingest(IngestRequest request) {
        PlatformType platform = mapPlatform(request.platformHeader());
        EventType eventType = mapEventType(request.eventTypeHeader());

        if (platform == null) {
            saveFailedEvent(request, "알 수 없는 플랫폼: " + request.platformHeader());
            return false;
        }

        PayloadTranslator translator = translators.get(platform);
        if (translator == null) {
            saveFailedEvent(request, "Translator 없음: " + platform);
            return false;
        }

        String rawPayload = extractRawPayload(request);
        if (rawPayload == null) {
            saveFailedEvent(request, "payload JSON 변환 실패");
            return false;
        }

        try {
            // 1단계: Translator로 정규화된 이벤트 생성
            ReservationEvent event = translator.translate(rawPayload, eventType);

            // 2단계: 예약 처리 서비스 호출
            ProcessingResult result = reservationProcessingService.process(event);

            if (!result.isSuccess()) {
                // ReservationProcessingService 내부에서 이미 실패 처리됨
                // FailedEventStore에는 별도 저장하지 않음 (ReservationEventEntity에 기록됨)
                return false;
            }

            return true;
        } catch (TranslationException e) {
            saveFailedEvent(request, e.getMessage());
            return false;
        }
    }

    private PlatformType mapPlatform(String header) {
        if (header == null) return null;
        return switch (header.toUpperCase()) {
            case "A", "YANOLJA" -> PlatformType.YANOLJA;
            case "B", "AIRBNB" -> PlatformType.AIRBNB;
            case "C", "YEOGIEOTTAE" -> PlatformType.YEOGIEOTTAE;
            default -> null;
        };
    }

    private EventType mapEventType(String header) {
        if (header == null) return EventType.BOOKING;
        return switch (header.toUpperCase()) {
            case "CANCEL", "CANCELLATION" -> EventType.CANCELLATION;
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
        String rawPayload = null;
        try {
            rawPayload = objectMapper.writeValueAsString(request.payload());
        } catch (JsonProcessingException e) {
            rawPayload = "직렬화 실패";
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
}
