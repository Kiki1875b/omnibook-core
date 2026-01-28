package simulator.report;

import simulator.platform.PlatformType;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects all events emitted during a scenario execution for reporting.
 */
public class ExecutionReport {

    private final String scenarioName;
    private final String correlationId;
    private final List<Entry> entries = new ArrayList<>();
    private long startTimeMs;
    private long endTimeMs;

    public ExecutionReport(String scenarioName, String correlationId) {
        this.scenarioName = scenarioName;
        this.correlationId = correlationId;
    }

    public void markStart() {
        this.startTimeMs = System.currentTimeMillis();
    }

    public void markEnd() {
        this.endTimeMs = System.currentTimeMillis();
    }

    public void addEntry(PlatformType platform, String eventType, String reservationId,
                         String chaosEffect, boolean delivered) {
        entries.add(new Entry(platform, eventType, reservationId, chaosEffect, delivered));
    }

    public String getScenarioName() { return scenarioName; }
    public String getCorrelationId() { return correlationId; }
    public List<Entry> getEntries() { return entries; }
    public long getDurationMs() { return endTimeMs - startTimeMs; }

    public static class Entry {
        private final PlatformType platform;
        private final String eventType;
        private final String reservationId;
        private final String chaosEffect;
        private final boolean delivered;

        public Entry(PlatformType platform, String eventType, String reservationId,
                     String chaosEffect, boolean delivered) {
            this.platform = platform;
            this.eventType = eventType;
            this.reservationId = reservationId;
            this.chaosEffect = chaosEffect;
            this.delivered = delivered;
        }

        public PlatformType getPlatform() { return platform; }
        public String getEventType() { return eventType; }
        public String getReservationId() { return reservationId; }
        public String getChaosEffect() { return chaosEffect; }
        public boolean isDelivered() { return delivered; }
    }
}
