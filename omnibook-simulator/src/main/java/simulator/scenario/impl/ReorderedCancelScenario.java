package simulator.scenario.impl;

import simulator.platform.PlatformType;
import simulator.platform.payload.YanoljaReservationPayload;
import simulator.platform.payload.YeogieottaeReservationPayload;
import simulator.scenario.Scenario;
import simulator.scenario.ScenarioContext;
import simulator.scenario.ScenarioResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario: Cancellation arrives before booking due to reordering.
 *
 * Yanolja and YeogiEottae both book and cancel.
 * Events are deliberately reordered so the broker receives CANCELLATION before BOOKING.
 * This tests whether the broker can handle out-of-order events.
 */
public class ReorderedCancelScenario implements Scenario {

    @Override
    public String name() {
        return "ReorderedCancel";
    }

    @Override
    public ScenarioResult execute(ScenarioContext ctx) {
        ctx.report().markStart();

        try {
            // Yanolja: book then cancel
            Object yanoljaBook = ctx.yanolja().book("R-301", "이서연", "2025-10-10", "2025-10-12");
            String yanoljaId = ((YanoljaReservationPayload) yanoljaBook).getReservationId();
            Object yanoljaCancel = ctx.yanolja().cancel(yanoljaId);

            // YeogiEottae: book then cancel
            Object yeogiBook = ctx.yeogieottae().book("R-301", "이서연", "2025-10-10", "2025-10-12");
            String yeogiId = ((YeogieottaeReservationPayload) yeogiBook).getOrderId();
            Object yeogiCancel = ctx.yeogieottae().cancel(yeogiId);

            // Build event list and reorder via chaos engine
            List<Runnable> events = new ArrayList<>();
            events.add(() -> ctx.emit(PlatformType.A, "BOOKING", yanoljaBook, yanoljaId));
            events.add(() -> ctx.emit(PlatformType.A, "CANCELLATION", yanoljaCancel, yanoljaId));
            events.add(() -> ctx.emit(PlatformType.C, "BOOKING", yeogiBook, yeogiId));
            events.add(() -> ctx.emit(PlatformType.C, "CANCELLATION", yeogiCancel, yeogiId));

            // Reorder the event list
            List<Runnable> reordered = ctx.chaosEngine().maybeReorder(events);
            for (Runnable r : reordered) {
                r.run();
            }

            ctx.report().markEnd();
            return ScenarioResult.ok(name(), ctx.report());
        } catch (Exception e) {
            ctx.report().markEnd();
            return ScenarioResult.fail(name(), ctx.report(), e.getMessage());
        }
    }
}
