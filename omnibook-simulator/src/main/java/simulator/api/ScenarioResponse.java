package simulator.api;

import simulator.report.ExecutionReport;
import simulator.scenario.ScenarioResult;

import java.util.List;

/**
 * JSON response returned after running a scenario via API.
 */
public class ScenarioResponse {

    private final String scenario;
    private final String correlationId;
    private final boolean success;
    private final String error;
    private final long durationMs;
    private final Summary summary;
    private final List<EventEntry> events;

    private ScenarioResponse(String scenario, String correlationId, boolean success,
                             String error, long durationMs, Summary summary,
                             List<EventEntry> events) {
        this.scenario = scenario;
        this.correlationId = correlationId;
        this.success = success;
        this.error = error;
        this.durationMs = durationMs;
        this.summary = summary;
        this.events = events;
    }

    public static ScenarioResponse from(ScenarioResult result) {
        ExecutionReport report = result.getReport();

        List<EventEntry> events = report.getEntries().stream()
                .map(e -> new EventEntry(
                        e.getPlatform().name(),
                        e.getPlatform().displayName(),
                        e.getEventType(),
                        e.getReservationId(),
                        e.getChaosEffect(),
                        e.isDelivered()))
                .toList();

        long total = events.size();
        long delivered = events.stream().filter(EventEntry::isDelivered).count();
        long failed = total - delivered;
        long chaotic = events.stream()
                .filter(e -> !"CLEAN".equals(e.getChaosEffect()))
                .count();

        return new ScenarioResponse(
                result.getScenarioName(),
                report.getCorrelationId(),
                result.isSuccess(),
                result.getErrorMessage(),
                report.getDurationMs(),
                new Summary(total, delivered, failed, chaotic),
                events);
    }

    // --- getters for Jackson ---

    public String getScenario() { return scenario; }
    public String getCorrelationId() { return correlationId; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public long getDurationMs() { return durationMs; }
    public Summary getSummary() { return summary; }
    public List<EventEntry> getEvents() { return events; }

    public static class Summary {
        private final long total;
        private final long delivered;
        private final long failed;
        private final long chaotic;

        public Summary(long total, long delivered, long failed, long chaotic) {
            this.total = total;
            this.delivered = delivered;
            this.failed = failed;
            this.chaotic = chaotic;
        }

        public long getTotal() { return total; }
        public long getDelivered() { return delivered; }
        public long getFailed() { return failed; }
        public long getChaotic() { return chaotic; }
    }

    public static class EventEntry {
        private final String platformCode;
        private final String platformName;
        private final String eventType;
        private final String reservationId;
        private final String chaosEffect;
        private final boolean delivered;

        public EventEntry(String platformCode, String platformName, String eventType,
                          String reservationId, String chaosEffect, boolean delivered) {
            this.platformCode = platformCode;
            this.platformName = platformName;
            this.eventType = eventType;
            this.reservationId = reservationId;
            this.chaosEffect = chaosEffect;
            this.delivered = delivered;
        }

        public String getPlatformCode() { return platformCode; }
        public String getPlatformName() { return platformName; }
        public String getEventType() { return eventType; }
        public String getReservationId() { return reservationId; }
        public String getChaosEffect() { return chaosEffect; }
        public boolean isDelivered() { return delivered; }
    }
}
