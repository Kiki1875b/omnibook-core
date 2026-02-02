package simulator.sender;

import simulator.event.PlatformEvent;

/**
 * 플랫폼 이벤트를 브로커로 전송하는 인터페이스.
 */
public interface EventSender {

    /**
     * PlatformEvent를 브로커로 전송한다.
     *
     * HTTP 전송 규칙:
     * Headers:
     * - X-Event-Id = event.eventId
     * - X-Platform = event.platform
     * - X-Event-Type = event.eventType
     * - X-Correlation-Id = event.correlationId
     *
     * Body:
     * {
     *   "eventId": "...",
     *   "reservationId": "...",
     *   "payload": { OTA payload JSON }
     * }
     *
     * @param targetUrl 브로커 엔드포인트 URL
     * @param event     전송할 이벤트
     * @return 전송 성공 여부 (HTTP 2xx면 true)
     */
    boolean send(String targetUrl, PlatformEvent event);
}
