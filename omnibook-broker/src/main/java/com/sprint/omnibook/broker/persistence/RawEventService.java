package com.sprint.omnibook.broker.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest;
import com.sprint.omnibook.broker.ingestion.EventHeaders;
import com.sprint.omnibook.broker.ingestion.IngestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 원본 이벤트 저장 서비스.
 * MongoDB Append-only SoT 역할을 수행한다.
 */
@Service
@RequiredArgsConstructor
public class RawEventService {

    private final RawEventRepository rawEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * raw body를 저장하고 IngestRequest로 변환한다.
     *
     * @param rawBody HTTP body 원본
     * @param headers HTTP 헤더 정보
     * @return Translator 처리용 IngestRequest
     */
    public IngestRequest receiveAndStore(String rawBody, EventHeaders headers) throws JsonProcessingException {
        IncomingEventRequest request = objectMapper.readValue(rawBody, IncomingEventRequest.class);
        String eventId = resolveEventId(headers.eventId(), request.getEventId());

        saveRawEvent(eventId, headers, rawBody);

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
        return bodyEventId;
    }

    private void saveRawEvent(String eventId, EventHeaders headers, String rawBody) {
        RawEventDocument document = RawEventDocument.builder()
                .eventId(eventId)
                .platform(headers.platform())
                .eventType(headers.eventType())
                .correlationId(headers.correlationId())
                .rawBody(rawBody)
                .receivedAt(Instant.now())
                .build();

        rawEventRepository.save(document);
    }
}
