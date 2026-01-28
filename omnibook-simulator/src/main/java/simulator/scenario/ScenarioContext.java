package simulator.scenario;

import simulator.chaos.ChaosDecision;
import simulator.chaos.ChaosEngine;
import simulator.platform.OtaPlatform;
import simulator.platform.PlatformType;
import simulator.report.ExecutionReport;
import simulator.sender.EventSender;

import java.util.Map;

/**
 * Shared context available to every scenario during execution.
 * Provides access to platforms, sender, chaos engine, and the execution report.
 */
public class ScenarioContext {

    private final Map<PlatformType, OtaPlatform> platforms;
    private final EventSender sender;
    private final ChaosEngine chaosEngine;
    private final String targetUrl;
    private final String correlationId;
    private final ExecutionReport report;

    public ScenarioContext(Map<PlatformType, OtaPlatform> platforms,
                           EventSender sender,
                           ChaosEngine chaosEngine,
                           String targetUrl,
                           String correlationId,
                           ExecutionReport report) {
        this.platforms = platforms;
        this.sender = sender;
        this.chaosEngine = chaosEngine;
        this.targetUrl = targetUrl;
        this.correlationId = correlationId;
        this.report = report;
    }

    public OtaPlatform platform(PlatformType type) {
        return platforms.get(type);
    }

    public OtaPlatform yanolja()     { return platforms.get(PlatformType.A); }
    public OtaPlatform airbnb()      { return platforms.get(PlatformType.B); }
    public OtaPlatform yeogieottae() { return platforms.get(PlatformType.C); }

    public ChaosEngine chaosEngine() { return chaosEngine; }
    public ExecutionReport report()  { return report; }
    public String correlationId()    { return correlationId; }

    /**
     * Emit a single event through the chaos + sender pipeline.
     * Chaos affects delivery only — never payload content.
     */
    public void emit(PlatformType platform, String eventType, Object payload, String reservationId) {
        ChaosDecision decision = chaosEngine.decide();
        String chaosTag = decision.toString();

        if (decision.isFail()) {
            System.out.printf("  [CHAOS] %s | %s | %s | SIMULATED FAILURE — not sending%n",
                    platform.displayName(), eventType, reservationId);
            report.addEntry(platform, eventType, reservationId, chaosTag, false);
            return;
        }

        if (decision.isDelay()) {
            System.out.printf("  [CHAOS] %s | %s | %s | DELAY %d ms%n",
                    platform.displayName(), eventType, reservationId, decision.getDelayMs());
            sleep(decision.getDelayMs());
        }

        int sends = 1 + (decision.isDuplicate() ? decision.getDuplicateCount() : 0);
        if (sends > 1) {
            System.out.printf("  [CHAOS] %s | %s | %s | DUPLICATE x%d%n",
                    platform.displayName(), eventType, reservationId, sends);
        }

        for (int i = 0; i < sends; i++) {
            boolean ok = sender.send(targetUrl, platform, eventType, payload, correlationId);
            report.addEntry(platform, eventType, reservationId,
                    i == 0 ? chaosTag : "DUP_COPY", ok);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
