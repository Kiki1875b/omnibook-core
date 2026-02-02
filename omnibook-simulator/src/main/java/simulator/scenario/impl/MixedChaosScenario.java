package simulator.scenario.impl;

import simulator.platform.PlatformType;
import simulator.platform.payload.AirbnbReservationPayload;
import simulator.platform.payload.YanoljaReservationPayload;
import simulator.platform.payload.YeogieottaeReservationPayload;
import simulator.scenario.Scenario;
import simulator.scenario.ScenarioContext;
import simulator.scenario.ScenarioResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario: All three platforms, all chaos types combined.
 *
 * Yanolja books and cancels same-day.
 * Airbnb books.
 * YeogiEottae books and modifies (re-book after cancel).
 *
 * Events are reordered via chaos engine.
 * Individual events are subject to duplication, delay, and failure.
 * This is the most adversarial scenario for the broker.
 */
public class MixedChaosScenario implements Scenario {

    @Override
    public String name() {
        return "MixedChaos";
    }

    @Override
    public ScenarioResult execute(ScenarioContext ctx) {
        ctx.report().markStart();

        try {
            // --- Yanolja: same-day book + cancel ---
            Object yanoljaBook = ctx.yanolja().book("R-501", "최동욱", "2025-12-24", "2025-12-25");
            String yanoljaId = ((YanoljaReservationPayload) yanoljaBook).getReservationId();
            Object yanoljaCancel = ctx.yanolja().cancel(yanoljaId);

            // --- Airbnb: book only ---
            Object airbnbBook = ctx.airbnb().book("R-501", "Dongwook Choi", "2025-12-24", "2025-12-26");
            String airbnbId = ((AirbnbReservationPayload) airbnbBook).getConfirmationCode();

            // --- YeogiEottae: book, cancel, re-book ---
            Object yeogiBook1 = ctx.yeogieottae().book("R-501", "최동욱", "2025-12-24", "2025-12-25");
            String yeogiId1 = ((YeogieottaeReservationPayload) yeogiBook1).getOrderId();
            Object yeogiCancel = ctx.yeogieottae().cancel(yeogiId1);
            Object yeogiBook2 = ctx.yeogieottae().book("R-501", "최동욱", "2025-12-24", "2025-12-26");
            String yeogiId2 = ((YeogieottaeReservationPayload) yeogiBook2).getOrderId();

            // Collect all events and reorder
            List<Runnable> events = new ArrayList<>();
            events.add(() -> ctx.emit(PlatformType.A, "BOOKING",       yanoljaBook,   yanoljaId));
            events.add(() -> ctx.emit(PlatformType.A, "CANCELLATION",  yanoljaCancel, yanoljaId));
            events.add(() -> ctx.emit(PlatformType.B, "BOOKING",       airbnbBook,    airbnbId));
            events.add(() -> ctx.emit(PlatformType.C, "BOOKING",       yeogiBook1,    yeogiId1));
            events.add(() -> ctx.emit(PlatformType.C, "CANCELLATION",  yeogiCancel,   yeogiId1));
            events.add(() -> ctx.emit(PlatformType.C, "BOOKING",       yeogiBook2,    yeogiId2));

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
