package simulator.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import simulator.platform.PlatformType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP JSON event sender.
 * Adds platform and correlation metadata as headers.
 */
public class HttpEventSender implements EventSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;

    public HttpEventSender() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public boolean send(String targetUrl, PlatformType platform, String eventType,
                        Object payload, String correlationId) {
        try {
            String json = MAPPER.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .header("X-Platform", platform.name())
                    .header("X-Platform-Name", platform.displayName())
                    .header("X-Event-Type", eventType)
                    .header("X-Correlation-Id", correlationId)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();

            System.out.printf("  [SEND] %s | %s | %s | HTTP %d%n",
                    platform.displayName(), eventType, correlationId, code);

            return code >= 200 && code < 300;
        } catch (Exception e) {
            System.out.printf("  [SEND] %s | %s | %s | FAILED: %s%n",
                    platform.displayName(), eventType, correlationId, e.getMessage());
            return false;
        }
    }
}
