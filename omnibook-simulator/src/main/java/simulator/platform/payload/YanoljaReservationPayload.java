package simulator.platform.payload;

import lombok.Data;

/**
 * Platform A — YANOLJA (Korean domestic OTA, mobile-first).
 */
@Data
public class YanoljaReservationPayload {

    private String reservationId;       // YNJ-xxxxxxxx
    private String roomId;
    private String roomName;
    private String accommodationName;
    private String accommodationAddress;

    private String checkInDate;         // yyyy-MM-dd
    private String checkOutDate;        // yyyy-MM-dd
    private int stayNights;

    private String guestName;
    private String guestPhone;          // 010-xxxx-xxxx format

    private String couponCode;          // nullable, promo/coupon applied
    private int discountAmount;         // KRW discount, 0 if no coupon

    private int totalPrice;             // KRW (integer, no decimals)
    private String paymentMethod;       // CARD | KAKAO_PAY | TOSS_PAY | NAVER_PAY

    private String status;              // 예약완료 | 취소 | 노쇼
    private String bookedAt;            // yyyy-MM-dd'T'HH:mm:ss
    private String platform;            // always "YANOLJA"
}
