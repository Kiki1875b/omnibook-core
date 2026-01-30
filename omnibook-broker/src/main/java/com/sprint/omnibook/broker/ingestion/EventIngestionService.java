package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.persistence.RawEventService;
import com.sprint.omnibook.broker.translator.PayloadTranslator;
import com.sprint.omnibook.broker.translator.TranslationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 이벤트 수신 및 변환 서비스.
 * MongoDB 저장 → Translator 처리 순서를 보장한다.
 */
@Service
public class EventIngestionService {

    private final RawEventService rawEventService;
    private final Map<PlatformType, PayloadTranslator> translators;
    private final FailedEventStore failedEventStore;
    private final ObjectMapper objectMapper;

    public EventIngestionService(
            RawEventService rawEventService,
            Map<PlatformType, PayloadTranslator> translators,
            FailedEventStore failedEventStore,
            ObjectMapper objectMapper) {
        this.rawEventService = rawEventService;
        this.translators = translators;
        this.failedEventStore = failedEventStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 이벤트 처리 진입점.
     * MongoDB 저장 → Translator 처리 순서를 보장한다.
     *
     * @param rawBody HTTP body 원본
     * @param headers HTTP 헤더 정보
     * @return 처리 결과
     */
    public IngestionResult process(String rawBody, EventHeaders headers) throws JsonProcessingException {
        IngestRequest request = rawEventService.receiveAndStore(rawBody, headers);
        boolean success = ingest(request);

        return new IngestionResult(request.eventId(), success);
    }

    /**
     * Translator 처리.
     *
     * @return 변환 성공 시 true, 실패 시 false (원본 저장됨)
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
            ReservationEvent event = translator.translate(rawPayload, eventType);
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
