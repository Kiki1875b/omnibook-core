package simulator.scenario.impl;

import simulator.platform.PlatformType;
import simulator.platform.payload.YanoljaReservationPayload;
import simulator.platform.payload.AirbnbReservationPayload;
import simulator.scenario.Scenario;
import simulator.scenario.ScenarioContext;
import simulator.scenario.ScenarioResult;

/**
 * Scenario: Duplicate/retry events from multiple platforms.
 *
 * Simulates what happens when OTAs retry webhook delivery due to timeouts.
 * Yanolja and Airbnb both book, and the same booking events are emitted multiple times
 * as if the OTA retried delivery.
 *
 * The broker must deduplicate.
 */
public class DuplicateRetryScenario implements Scenario {

    @Override
    public String name() {
        return "DuplicateRetry";
    }

    @Override
    public ScenarioResult execute(ScenarioContext ctx) {
        ctx.report().markStart();

        try {
            // Yanolja books
            Object yanoljaPayload = ctx.yanolja().book("R-201", "박지현", "2025-09-01", "2025-09-03");
            String yanoljaId = ((YanoljaReservationPayload) yanoljaPayload).getReservationId();

            // Emit original + manual retries (simulating OTA webhook retry behavior)
            ctx.emit(PlatformType.A, "BOOKING", yanoljaPayload, yanoljaId);
            ctx.emit(PlatformType.A, "BOOKING", yanoljaPayload, yanoljaId); // retry 1
            ctx.emit(PlatformType.A, "BOOKING", yanoljaPayload, yanoljaId); // retry 2

            // Airbnb books
            Object airbnbPayload = ctx.airbnb().book("R-201", "Jihyun Park", "2025-09-01", "2025-09-03");
            String airbnbId = ((AirbnbReservationPayload) airbnbPayload).getConfirmationCode();

            // Emit original + retry
            ctx.emit(PlatformType.B, "BOOKING", airbnbPayload, airbnbId);
            ctx.emit(PlatformType.B, "BOOKING", airbnbPayload, airbnbId); // retry 1

            ctx.report().markEnd();
            return ScenarioResult.ok(name(), ctx.report());
        } catch (Exception e) {
            ctx.report().markEnd();
            return ScenarioResult.fail(name(), ctx.report(), e.getMessage());
        }
    }
}
