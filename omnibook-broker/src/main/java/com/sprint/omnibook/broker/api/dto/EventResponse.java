package com.sprint.omnibook.broker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이벤트 수신 응답.
 */
@Getter
@AllArgsConstructor
public class EventResponse {

    private final String eventId;
    private final String status;
    private final String message;

    public static EventResponse accepted(String eventId) {
        return new EventResponse(eventId, "ACCEPTED", "이벤트가 정상 처리되었습니다.");
    }

    public static EventResponse failed(String eventId, String reason) {
        return new EventResponse(eventId, "FAILED", reason);
    }

    public static EventResponse savedForRetry(String eventId) {
        return new EventResponse(eventId, "SAVED_FOR_RETRY", "변환 실패. 원본이 저장되었습니다.");
    }
}
