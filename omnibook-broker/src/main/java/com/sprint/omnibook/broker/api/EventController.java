package com.sprint.omnibook.broker.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sprint.omnibook.broker.api.dto.ErrorResponse;
import com.sprint.omnibook.broker.api.dto.EventResponse;
import com.sprint.omnibook.broker.ingestion.EventHeaders;
import com.sprint.omnibook.broker.ingestion.EventIngestionService;
import com.sprint.omnibook.broker.ingestion.EventTypeHeaderAlias;
import com.sprint.omnibook.broker.ingestion.IngestionResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 외부 이벤트 수신 컨트롤러.
 * HTTP 요청/응답 처리만 담당하는 thin layer.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private static final String MDC_CORRELATION_ID = "correlationId";

    private final EventIngestionService ingestionService;

    /**
     * 외부 플랫폼으로부터 예약 이벤트를 수신하고 처리 결과를 반환한다.
     */
    @PostMapping
    public ResponseEntity<?> receiveEvent(
            @RequestHeader(value = "X-Event-Id", required = false) String eventId,
            @RequestHeader(value = "X-Platform", required = true) String platform,
            @RequestHeader(value = "X-Event-Type", required = false, defaultValue = EventTypeHeaderAlias.BOOKING) String eventType,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @RequestBody String rawBody) throws JsonProcessingException {

        EventHeaders headers = new EventHeaders(eventId, platform, eventType, correlationId);
        IngestionResult result = ingestionService.process(rawBody, headers);

        if (result.success()) {
            return ResponseEntity.ok(EventResponse.accepted(result.eventId()));
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                result.errorCode(),
                result.failureReason(),
                Map.of("eventId", result.eventId()),
                MDC.get(MDC_CORRELATION_ID)
        );

        return ResponseEntity
                .status(result.errorCode().getHttpStatus())
                .body(errorResponse);
    }
}
