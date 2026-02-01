package com.sprint.omnibook.broker.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sprint.omnibook.broker.api.dto.EventResponse;
import com.sprint.omnibook.broker.ingestion.EventHeaders;
import com.sprint.omnibook.broker.ingestion.EventIngestionService;
import com.sprint.omnibook.broker.ingestion.IngestionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 외부 이벤트 수신 컨트롤러.
 * HTTP 요청/응답 처리만 담당하는 thin layer.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventIngestionService ingestionService;

    @PostMapping
    public ResponseEntity<EventResponse> receiveEvent(
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-Platform", required = true) String platform,
            @RequestHeader(value = "X-Event-Type", required = false, defaultValue = "BOOKING") String eventType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody String rawBody) throws JsonProcessingException {

        EventHeaders headers = new EventHeaders(eventId, platform, eventType, correlationId);
        IngestionResult result = ingestionService.process(rawBody, headers);

        if (result.success()) {
            return ResponseEntity.ok(EventResponse.accepted(result.eventId()));
        } else {
            return ResponseEntity.accepted()
                    .body(EventResponse.savedForRetry(result.eventId()));
        }
    }
}
