package simulator.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import simulator.report.ExecutionReport;
import simulator.scenario.ScenarioResult;

import java.util.List;

/**
 * JSON response returned after running a scenario via API.
 */
@Getter
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ScenarioResponse {

    private final String scenario;
    private final String correlationId;
    private final boolean success;
    private final String error;
    private final long durationMs;
    private final Summary summary;
    private final List<EventEntry> events;

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

    @Getter
    @AllArgsConstructor
    public static class Summary {
        private final long total;
        private final long delivered;
        private final long failed;
        private final long chaotic;
    }

    @Getter
    @AllArgsConstructor
    public static class EventEntry {
        private final String platformCode;
        private final String platformName;
        private final String eventType;
        private final String reservationId;
        private final String chaosEffect;
        private final boolean delivered;
    }
}
