package simulator.scenario.impl;

import simulator.platform.PlatformType;
import simulator.platform.payload.AirbnbReservationPayload;
import simulator.platform.payload.YeogieottaeReservationPayload;
import simulator.scenario.Scenario;
import simulator.scenario.ScenarioContext;
import simulator.scenario.ScenarioResult;

/**
 * Scenario: Delayed delivery from batch-oriented platform.
 *
 * Airbnb event arrives promptly.
 * YeogiEottae events arrive late (simulating its batch-oriented nature).
 * Both platforms book the same room/dates — the broker sees a timing gap.
 */
public class DelayedDeliveryScenario implements Scenario {

    @Override
    public String name() {
        return "DelayedDelivery";
    }

    @Override
    public ScenarioResult execute(ScenarioContext ctx) {
        ctx.report().markStart();

        try {
            // Airbnb books first — delivered immediately
            Object airbnbPayload = ctx.airbnb().book("R-401", "Yuna Lee", "2025-11-20", "2025-11-23");
            String airbnbId = ((AirbnbReservationPayload) airbnbPayload).getConfirmationCode();
            ctx.emit(PlatformType.B, "BOOKING", airbnbPayload, airbnbId);

            // YeogiEottae books the same room — delivery is delayed (simulated batch lag)
            Object yeogiPayload = ctx.yeogieottae().book("R-401", "이유나", "2025-11-20", "2025-11-23");
            String yeogiId = ((YeogieottaeReservationPayload) yeogiPayload).getOrderId();

            System.out.printf("  [SCENARIO] Simulating YeogiEottae batch delay for %s%n", yeogiId);
            sleep(2000); // hard-coded batch delay to demonstrate timing gap

            ctx.emit(PlatformType.C, "BOOKING", yeogiPayload, yeogiId);

            // YeogiEottae then cancels — also delayed
            Object yeogiCancel = ctx.yeogieottae().cancel(yeogiId);
            sleep(1500);
            ctx.emit(PlatformType.C, "CANCELLATION", yeogiCancel, yeogiId);

            ctx.report().markEnd();
            return ScenarioResult.ok(name(), ctx.report());
        } catch (Exception e) {
            ctx.report().markEnd();
            return ScenarioResult.fail(name(), ctx.report(), e.getMessage());
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
