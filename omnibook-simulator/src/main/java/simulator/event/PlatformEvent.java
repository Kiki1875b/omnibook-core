package simulator.event;

import simulator.platform.PlatformType;

/**
 * Event Envelope - 플랫폼 이벤트의 전송 단위.
 *
 * 핵심 원칙:
 * - eventId: "이 전송 시도 자체의 식별자" (Chaos duplicate/retry 시에도 동일)
 * - reservationId: "예약의 ID" (eventId와 별개)
 *
 * 불변 객체.
 */
public final class PlatformEvent {

    private final String eventId;
    private final PlatformType platform;
    private final String eventType;
    private final String reservationId;
    private final Object payload;
    private final String correlationId;

    public PlatformEvent(String eventId, PlatformType platform, String eventType,
                         String reservationId, Object payload, String correlationId) {
        this.eventId = eventId;
        this.platform = platform;
        this.eventType = eventType;
        this.reservationId = reservationId;
        this.payload = payload;
        this.correlationId = correlationId;
    }

    public String getEventId() {
        return eventId;
    }

    public PlatformType getPlatform() {
        return platform;
    }

    public String getEventType() {
        return eventType;
    }

    public String getReservationId() {
        return reservationId;
    }

    public Object getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
