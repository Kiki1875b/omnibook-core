package simulator.platform;

import org.springframework.stereotype.Component;
import simulator.platform.payload.YeogieottaeReservationPayload;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Platform C — YEOGIEOTTAE.
 * Korean domestic OTA. Batch-oriented delivery.
 * Coarser status granularity. Delayed/reordered events are common.
 * Compact date format (yyyyMMdd). Epoch SECONDS for timestamps.
 */
@Component
public class YeogieottaePlatform extends AbstractOtaPlatform {

    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String[] PAY_METHODS = {"CARD", "PHONE", "BANK_TRANSFER"};
    private static final String[] ROOM_TYPE_NAMES = {"스탠다드", "디럭스", "트윈", "온돌방", "복층"};

    @Override
    public PlatformType type() {
        return PlatformType.C;
    }

    @Override
    public Object book(String roomId, String guestName, String checkIn, String checkOut) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        String orderId = generateId("YEO-");
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        long nowEpochSec = Instant.now().getEpochSecond();

        int nights = (int) (out.toEpochDay() - in.toEpochDay());
        int pricePerNight = 40_000 + rng.nextInt(120_000);

        YeogieottaeReservationPayload p = new YeogieottaeReservationPayload();
        p.setOrderId(orderId);
        p.setAccommodationId("ACC-" + (1000 + rng.nextInt(9000)));
        p.setRoomTypeId("RT-" + roomId);
        p.setRoomTypeName(ROOM_TYPE_NAMES[rng.nextInt(ROOM_TYPE_NAMES.length)]);
        p.setStartDate(in.format(COMPACT_DATE));
        p.setEndDate(out.format(COMPACT_DATE));
        p.setBuyerName(guestName);
        p.setBuyerTel("010" + (10000000 + rng.nextInt(90000000)));
        p.setTotalAmount(nights * pricePerNight);
        p.setPayMethod(PAY_METHODS[rng.nextInt(PAY_METHODS.length)]);
        p.setState(1); // 1 = BOOKED
        p.setRegisteredTs(nowEpochSec);
        p.setLastModifiedTs(nowEpochSec);

        storeReservation(orderId, p);
        return p;
    }

    @Override
    public Object cancel(String reservationId) {
        Object existing = removeReservation(reservationId);
        if (existing == null) {
            YeogieottaeReservationPayload p = new YeogieottaeReservationPayload();
            p.setOrderId(reservationId);
            p.setState(2); // 2 = CANCELLED
            p.setLastModifiedTs(Instant.now().getEpochSecond());
            return p;
        }
        YeogieottaeReservationPayload p = (YeogieottaeReservationPayload) existing;
        p.setState(2); // 2 = CANCELLED
        p.setLastModifiedTs(Instant.now().getEpochSecond());
        return p;
    }
}
