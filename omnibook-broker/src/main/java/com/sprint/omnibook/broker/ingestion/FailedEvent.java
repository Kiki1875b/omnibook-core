package com.sprint.omnibook.broker.ingestion;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 변환 실패한 이벤트 원본 저장용.
 * 추후 재처리를 위해 원본 정보를 보존한다.
 */
@Getter
@Builder
public class FailedEvent {

    private final String eventId;
    private final String platform;
    private final String eventType;
    private final String correlationId;
    private final String reservationId;
    private final String rawPayload;
    private final String errorMessage;
    private final Instant failedAt;
}
