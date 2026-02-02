package simulator.scenario;

import lombok.RequiredArgsConstructor;
import simulator.chaos.ChaosDecision;
import simulator.chaos.ChaosEngine;
import simulator.event.PlatformEvent;
import simulator.platform.OtaPlatform;
import simulator.platform.PlatformType;
import simulator.report.ExecutionReport;
import simulator.sender.EventSender;

import java.util.Map;
import java.util.UUID;

/**
 * Shared context available to every scenario during execution.
 * Provides access to platforms, sender, chaos engine, and the execution report.
 */
@RequiredArgsConstructor
public class ScenarioContext {

    private final Map<PlatformType, OtaPlatform> platforms;
    private final EventSender sender;
    private final ChaosEngine chaosEngine;
    private final String targetUrl;
    private final String correlationId;
    private final ExecutionReport report;

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
     *
     * 내부에서 UUID를 생성하여 PlatformEvent를 만들고 emitEvent()로 위임.
     */
    public void emit(PlatformType platform, String eventType, Object payload, String reservationId) {
        String eventId = UUID.randomUUID().toString();
        PlatformEvent event = new PlatformEvent(
                eventId, platform, eventType, reservationId, payload, correlationId
        );
        emitEvent(event);
    }

    /**
     * PlatformEvent 단위로 Chaos + Sender 로직 처리.
     * 중복 전송 시 동일한 eventId를 가진 PlatformEvent가 여러 번 전송된다.
     */
    public void emitEvent(PlatformEvent event) {
        ChaosDecision decision = chaosEngine.decide();
        String chaosTag = decision.toString();

        if (decision.isFail()) {
            System.out.printf("  [CHAOS] %s | %s | eventId=%s | SIMULATED FAILURE — not sending%n",
                    event.getPlatform().displayName(), event.getEventType(), event.getEventId());
            report.addEntry(event.getPlatform(), event.getEventType(),
                    event.getEventId(), event.getReservationId(), chaosTag, false);
            return;
        }

        if (decision.isDelay()) {
            System.out.printf("  [CHAOS] %s | %s | eventId=%s | DELAY %d ms%n",
                    event.getPlatform().displayName(), event.getEventType(),
                    event.getEventId(), decision.getDelayMs());
            sleep(decision.getDelayMs());
        }

        int sends = 1 + (decision.isDuplicate() ? decision.getDuplicateCount() : 0);
        if (sends > 1) {
            System.out.printf("  [CHAOS] %s | %s | eventId=%s | DUPLICATE x%d%n",
                    event.getPlatform().displayName(), event.getEventType(),
                    event.getEventId(), sends);
        }

        // 중복 전송 시 동일한 eventId로 여러 번 전송
        for (int i = 0; i < sends; i++) {
            boolean ok = sender.send(targetUrl, event);
            report.addEntry(event.getPlatform(), event.getEventType(),
                    event.getEventId(), event.getReservationId(),
                    i == 0 ? chaosTag : "DUP_COPY", ok);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
