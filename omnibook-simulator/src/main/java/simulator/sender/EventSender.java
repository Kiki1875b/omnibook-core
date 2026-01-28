package simulator.sender;

import simulator.platform.PlatformType;

/**
 * Sends a platform event to the broker over HTTP.
 */
public interface EventSender {

    /**
     * @param targetUrl     broker endpoint URL
     * @param platform      source platform (A/B/C)
     * @param eventType     BOOKING or CANCELLATION
     * @param payload       platform-specific payload object (will be JSON-serialized)
     * @param correlationId groups all events belonging to one scenario run
     * @return true if delivery succeeded (HTTP 2xx), false otherwise
     */
    boolean send(String targetUrl, PlatformType platform, String eventType,
                 Object payload, String correlationId);
}
