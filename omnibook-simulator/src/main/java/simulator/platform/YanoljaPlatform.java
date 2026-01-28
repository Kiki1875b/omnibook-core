package simulator.platform;

import simulator.platform.payload.YanoljaReservationPayload;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Platform A — YANOLJA.
 * Korean domestic mobile-first OTA.
 * Frequent same-day book/cancel. Coupon/promo heavy. KRW integer pricing.
 */
public class YanoljaPlatform extends AbstractOtaPlatform {

    private static final String[] PAYMENT_METHODS = {"CARD", "KAKAO_PAY", "TOSS_PAY", "NAVER_PAY"};
    private static final String[] COUPONS = {null, null, "WELCOME10", "SUMMER2025", "FLASH50", null};
    private static final String[] ROOM_NAMES = {"스탠다드룸", "디럭스룸", "스위트룸", "패밀리룸", "프리미엄룸"};
    private static final String[] ACCOM_NAMES = {"서울 시티 호텔", "제주 오션 리조트", "부산 해운대 모텔", "강남 비즈니스 호텔"};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public PlatformType type() {
        return PlatformType.A;
    }

    @Override
    public Object book(String roomId, String guestName, String checkIn, String checkOut) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        String resId = generateId("YNJ-");
        LocalDate in = LocalDate.parse(checkIn);
        LocalDate out = LocalDate.parse(checkOut);
        int nights = (int) ChronoUnit.DAYS.between(in, out);

        String coupon = COUPONS[rng.nextInt(COUPONS.length)];
        int basePrice = nights * (50_000 + rng.nextInt(150_000));
        int discount = coupon != null ? (basePrice * (10 + rng.nextInt(20)) / 100) : 0;

        YanoljaReservationPayload p = new YanoljaReservationPayload();
        p.setReservationId(resId);
        p.setRoomId(roomId);
        p.setRoomName(ROOM_NAMES[rng.nextInt(ROOM_NAMES.length)]);
        p.setAccommodationName(ACCOM_NAMES[rng.nextInt(ACCOM_NAMES.length)]);
        p.setCheckInDate(in.format(DATE_FMT));
        p.setCheckOutDate(out.format(DATE_FMT));
        p.setStayNights(nights);
        p.setGuestName(guestName);
        p.setGuestPhone("010-" + (1000 + rng.nextInt(9000)) + "-" + (1000 + rng.nextInt(9000)));
        p.setCouponCode(coupon);
        p.setDiscountAmount(discount);
        p.setTotalPrice(basePrice - discount);
        p.setPaymentMethod(PAYMENT_METHODS[rng.nextInt(PAYMENT_METHODS.length)]);
        p.setStatus("예약완료");
        p.setBookedAt(LocalDateTime.now().format(DATETIME_FMT));
        p.setPlatform("YANOLJA");

        storeReservation(resId, p);
        return p;
    }

    @Override
    public Object cancel(String reservationId) {
        Object existing = removeReservation(reservationId);
        if (existing == null) {
            // optimistic emit — Yanolja sometimes fires cancel even if already gone
            YanoljaReservationPayload p = new YanoljaReservationPayload();
            p.setReservationId(reservationId);
            p.setStatus("취소");
            p.setBookedAt(LocalDateTime.now().format(DATETIME_FMT));
            p.setPlatform("YANOLJA");
            return p;
        }
        YanoljaReservationPayload p = (YanoljaReservationPayload) existing;
        p.setStatus("취소");
        return p;
    }
}
