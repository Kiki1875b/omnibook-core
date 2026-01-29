package simulator.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import simulator.event.PlatformEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP JSON event sender.
 *
 * 전송 규칙:
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
 */
@Component
public class HttpEventSender implements EventSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;

    public HttpEventSender() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean send(String targetUrl, PlatformEvent event) {
        try {
            // Body 구성: eventId, reservationId, payload
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", event.getEventId());
            body.put("reservationId", event.getReservationId());
            body.put("payload", event.getPayload());

            String json = MAPPER.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .header("X-Event-Id", event.getEventId())
                    .header("X-Platform", event.getPlatform().name())
                    .header("X-Event-Type", event.getEventType())
                    .header("X-Correlation-Id", event.getCorrelationId())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            System.out.printf("  [SEND] %s | %s | eventId=%s | HTTP %d%n",
                    event.getPlatform().displayName(), event.getEventType(),
                    event.getEventId(), code);

            return code >= 200 && code < 300;
        } catch (Exception e) {
            System.out.printf("  [SEND] %s | %s | eventId=%s | FAILED: %s%n",
                    event.getPlatform().displayName(), event.getEventType(),
                    event.getEventId(), e.getMessage());
            return false;
        }
    }
}
