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

    /**
     * 성공 응답을 생성한다.
     */
    public static EventResponse accepted(String eventId) {
        return new EventResponse(eventId, Status.ACCEPTED, Message.ACCEPTED);
    }

    /**
     * 실패 응답을 생성한다.
     */
    public static EventResponse failed(String eventId, String reason) {
        return new EventResponse(eventId, Status.FAILED, reason);
    }

    /**
     * 재시도 대기 응답을 생성한다.
     */
    public static EventResponse savedForRetry(String eventId) {
        return new EventResponse(eventId, Status.SAVED_FOR_RETRY, Message.SAVED_FOR_RETRY);
    }

    /**
     * 사유를 포함한 재시도 대기 응답을 생성한다.
     */
    public static EventResponse savedForRetry(String eventId, String reason) {
        return new EventResponse(eventId, Status.SAVED_FOR_RETRY, reason);
    }

    private static class Status {
        static final String ACCEPTED = "ACCEPTED";
        static final String FAILED = "FAILED";
        static final String SAVED_FOR_RETRY = "SAVED_FOR_RETRY";
    }

    private static class Message {
        static final String ACCEPTED = "이벤트가 정상 처리되었습니다.";
        static final String SAVED_FOR_RETRY = "변환 실패. 원본이 저장되었습니다.";
    }
}
