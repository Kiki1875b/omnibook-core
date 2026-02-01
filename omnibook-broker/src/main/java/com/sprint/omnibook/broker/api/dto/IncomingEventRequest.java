package com.sprint.omnibook.broker.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시뮬레이터로부터 수신하는 이벤트 요청 바디.
 *
 * Body 형식:
 * {
 *   "eventId": "...",
 *   "reservationId": "...",
 *   "payload": { OTA specific JSON }
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class IncomingEventRequest {

    private String eventId;
    private String reservationId;
    private JsonNode payload;
}
