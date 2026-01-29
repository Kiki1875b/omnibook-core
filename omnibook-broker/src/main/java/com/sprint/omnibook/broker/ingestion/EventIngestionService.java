package com.sprint.omnibook.broker.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.event.EventType;
import com.sprint.omnibook.broker.event.PlatformType;
import com.sprint.omnibook.broker.event.ReservationEvent;
import com.sprint.omnibook.broker.translator.PayloadTranslator;
import com.sprint.omnibook.broker.translator.TranslationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * 이벤트 수신 및 변환 서비스.
 *
 * 플랫폼별 Translator를 라우팅하고, 실패 시 원본을 저장한다.
 */
@Slf4j
@Service
public class EventIngestionService {

    private final Map<PlatformType, PayloadTranslator> translators;
    private final FailedEventStore failedEventStore;
    private final ObjectMapper objectMapper;

    public EventIngestionService(
            Map<PlatformType, PayloadTranslator> translators,
            FailedEventStore failedEventStore,
            ObjectMapper objectMapper) {
        this.translators = translators;
        this.failedEventStore = failedEventStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 이벤트 수신 처리.
     *
     * @return 변환 성공 시 true, 실패 시 false (원본 저장됨)
     */
    public boolean ingest(IngestRequest request) {
        PlatformType platform = mapPlatform(request.platformHeader());
        EventType eventType = mapEventType(request.eventTypeHeader());

        if (platform == null) {
            log.error("[Ingestion] 알 수 없는 플랫폼: {}", request.platformHeader());
            saveFailedEvent(request, "알 수 없는 플랫폼: " + request.platformHeader());
            return false;
        }

        PayloadTranslator translator = translators.get(platform);
        if (translator == null) {
            log.error("[Ingestion] Translator 없음: {}", platform);
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
            logSuccess(request, event);
            return true;
        } catch (TranslationException e) {
            log.error("[Ingestion] 변환 실패: eventId={}, error={}", request.eventId(), e.getMessage());
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
            log.error("[Ingestion] payload 직렬화 실패: {}", e.getMessage());
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

    private void logSuccess(IngestRequest request, ReservationEvent event) {
        log.info("[Ingestion] 성공: eventId={}, platform={}, reservationId={}, status={}",
                request.eventId(),
                event.getPlatformType(),
                event.getPlatformReservationId(),
                event.getStatus());
    }
}
