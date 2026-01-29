package com.sprint.omnibook.broker.api;

import com.sprint.omnibook.broker.api.dto.EventResponse;
import com.sprint.omnibook.broker.api.dto.IncomingEventRequest;
import com.sprint.omnibook.broker.ingestion.EventIngestionService;
import com.sprint.omnibook.broker.ingestion.IngestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 외부 이벤트 수신 컨트롤러.
 *
 * 시뮬레이터 또는 실제 OTA 웹훅으로부터 이벤트를 수신한다.
 *
 * Headers:
 * - X-Event-Id: 이벤트 식별자
 * - X-Platform: A | B | C (또는 YANOLJA | AIRBNB | YEOGIEOTTAE)
 * - X-Event-Type: BOOKING | CANCEL
 * - X-Correlation-Id: 상관관계 ID
 *
 * Body:
 * {
 *   "eventId": "...",
 *   "reservationId": "...",
 *   "payload": { OTA specific JSON }
 * }
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventIngestionService ingestionService;

    @PostMapping
    public ResponseEntity<EventResponse> receiveEvent(
            @RequestHeader(value = "X-Event-Id", required = false) String headerEventId,
            @RequestHeader(value = "X-Platform", required = true) String platform,
            @RequestHeader(value = "X-Event-Type", required = false, defaultValue = "BOOKING") String eventType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody IncomingEventRequest request) {

        String eventId = resolveEventId(headerEventId, request.getEventId());

        log.info("[EventController] 수신: eventId={}, platform={}, eventType={}, correlationId={}",
                eventId, platform, eventType, correlationId);

        IngestRequest ingestRequest = new IngestRequest(
                eventId,
                platform,
                eventType,
                correlationId,
                request.getReservationId(),
                request.getPayload()
        );

        boolean success = ingestionService.ingest(ingestRequest);

        if (success) {
            return ResponseEntity.ok(EventResponse.accepted(eventId));
        } else {
            return ResponseEntity.accepted()
                    .body(EventResponse.savedForRetry(eventId));
        }
    }

    private String resolveEventId(String headerEventId, String bodyEventId) {
        if (headerEventId != null && !headerEventId.isBlank()) {
            return headerEventId;
        }
        return bodyEventId;
    }
}
