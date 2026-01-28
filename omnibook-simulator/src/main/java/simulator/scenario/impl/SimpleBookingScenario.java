package simulator.scenario.impl;

import simulator.platform.PlatformType;
import simulator.scenario.Scenario;
import simulator.scenario.ScenarioContext;
import simulator.scenario.ScenarioResult;

/**
 * Scenario: Simple cross-platform booking.
 *
 * A guest books the same dates across Yanolja, Airbnb, and YeogiEottae simultaneously.
 * This is the baseline scenario — tests that all three platform payloads arrive at the broker
 * with their distinct shapes.
 *
 * Chaos: applied via context (standard delivery disruption).
 */
public class SimpleBookingScenario implements Scenario {

    @Override
    public String name() {
        return "SimpleBooking";
    }

    @Override
    public ScenarioResult execute(ScenarioContext ctx) {
        ctx.report().markStart();

        try {
            String roomId = "R-101";
            String checkIn = "2025-08-15";
            String checkOut = "2025-08-18";

            // Yanolja booking
            Object yanoljaPayload = ctx.yanolja().book(roomId, "김민수", checkIn, checkOut);
            String yanoljaId = extractId(yanoljaPayload, "YNJ");
            ctx.emit(PlatformType.A, "BOOKING", yanoljaPayload, yanoljaId);

            // Airbnb booking
            Object airbnbPayload = ctx.airbnb().book(roomId, "Minsu Kim", checkIn, checkOut);
            String airbnbId = extractId(airbnbPayload, "AIRBNB");
            ctx.emit(PlatformType.B, "BOOKING", airbnbPayload, airbnbId);

            // YeogiEottae booking
            Object yeogiPayload = ctx.yeogieottae().book(roomId, "김민수", checkIn, checkOut);
            String yeogiId = extractId(yeogiPayload, "YEO");
            ctx.emit(PlatformType.C, "BOOKING", yeogiPayload, yeogiId);

            ctx.report().markEnd();
            return ScenarioResult.ok(name(), ctx.report());
        } catch (Exception e) {
            ctx.report().markEnd();
            return ScenarioResult.fail(name(), ctx.report(), e.getMessage());
        }
    }

    private String extractId(Object payload, String hint) {
        try {
            var method = payload.getClass().getMethod(
                    hint.equals("YNJ") ? "getReservationId" :
                    hint.equals("AIRBNB") ? "getConfirmationCode" :
                    "getOrderId");
            return (String) method.invoke(payload);
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
