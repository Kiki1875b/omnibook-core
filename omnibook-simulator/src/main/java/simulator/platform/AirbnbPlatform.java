package simulator.platform;

import org.springframework.stereotype.Component;
import simulator.platform.payload.AirbnbReservationPayload;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Platform B — AIRBNB.
 * Global host-based OTA. Nights-based model.
 * Strict check-in/check-out. Fewer modifications. Timezone-aware.
 */
@Component
public class AirbnbPlatform extends AbstractOtaPlatform {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String[] TIMEZONES = {"Asia/Seoul", "America/New_York", "Europe/London", "Asia/Tokyo", "America/Los_Angeles"};
    private static final String[] CANCEL_POLICIES = {"FLEXIBLE", "MODERATE", "STRICT"};
    private static final String[] CURRENCIES = {"USD", "KRW", "EUR", "JPY"};

    @Override
    public PlatformType type() {
        return PlatformType.B;
    }

    @Override
    public Object book(String roomId, String guestName, String checkIn, String checkOut) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        String code = generateConfirmationCode(rng);
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        int nights = (int) ChronoUnit.DAYS.between(in, out);

        String[] nameParts = splitGuestName(guestName);
        long now = System.currentTimeMillis();

        int adults = 1 + rng.nextInt(3);
        int children = rng.nextInt(3);
        int infants = rng.nextBoolean() ? 0 : rng.nextInt(2);

        String currency = CURRENCIES[rng.nextInt(CURRENCIES.length)];
        double nightlyRate = 80.0 + rng.nextDouble(300.0);
        double cleaning = 30.0 + rng.nextDouble(70.0);
        double subtotal = nightlyRate * nights + cleaning;
        double guestFee = subtotal * 0.14;
        double hostFee = subtotal * 0.03;

        AirbnbReservationPayload p = new AirbnbReservationPayload();
        p.setConfirmationCode(code);
        p.setListingId("LST-" + roomId);
        p.setHostId("HOST-" + (1000 + rng.nextInt(9000)));
        p.setCheckIn(checkIn);
        p.setCheckOut(checkOut);
        p.setNights(nights);
        p.setGuestFirstName(nameParts[0]);
        p.setGuestLastName(nameParts[1]);
        p.setGuestEmail(nameParts[0].toLowerCase() + rng.nextInt(100) + "@email.com");
        p.setNumberOfGuests(adults + children + infants);
        p.setNumberOfAdults(adults);
        p.setNumberOfChildren(children);
        p.setNumberOfInfants(infants);
        p.setTotalPayout(round2(subtotal - hostFee));
        p.setHostServiceFee(round2(hostFee));
        p.setGuestServiceFee(round2(guestFee));
        p.setCleaningFee(round2(cleaning));
        p.setCurrency(currency);
        p.setStatus("ACCEPTED");
        p.setCancellationPolicy(CANCEL_POLICIES[rng.nextInt(CANCEL_POLICIES.length)]);
        p.setTimezone(TIMEZONES[rng.nextInt(TIMEZONES.length)]);
        p.setCreatedAt(now);
        p.setUpdatedAt(now);

        storeReservation(code, p);
        return p;
    }

    @Override
    public Object cancel(String reservationId) {
        Object existing = removeReservation(reservationId);
        if (existing == null) {
            AirbnbReservationPayload p = new AirbnbReservationPayload();
            p.setConfirmationCode(reservationId);
            p.setStatus("CANCELLED");
            p.setUpdatedAt(System.currentTimeMillis());
            return p;
        }
        AirbnbReservationPayload p = (AirbnbReservationPayload) existing;
        p.setStatus("CANCELLED");
        p.setUpdatedAt(System.currentTimeMillis());
        return p;
    }

    private String generateConfirmationCode(ThreadLocalRandom rng) {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private String[] splitGuestName(String name) {
        int space = name.indexOf(' ');
        if (space > 0) {
            return new String[]{name.substring(0, space), name.substring(space + 1)};
        }
        return new String[]{name, ""};
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
