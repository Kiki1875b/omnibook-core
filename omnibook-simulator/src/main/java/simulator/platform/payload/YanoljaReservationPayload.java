package simulator.platform.payload;

/**
 * Platform A — YANOLJA (Korean domestic OTA, mobile-first).
 *
 * Characteristics:
 * - Human-readable date strings (yyyy-MM-dd)
 * - Korean-style guest info (name + phone, no email)
 * - Promotion / coupon fields
 * - Status as Korean string constants
 * - Frequent same-day book/cancel cycles
 */
public class YanoljaReservationPayload {

    private String reservationId;       // YNJ-xxxxxxxx
    private String roomId;
    private String roomName;
    private String accommodationName;

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

    public YanoljaReservationPayload() {}

    // --- getters / setters ---

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getAccommodationName() { return accommodationName; }
    public void setAccommodationName(String accommodationName) { this.accommodationName = accommodationName; }

    public String getCheckInDate() { return checkInDate; }
    public void setCheckInDate(String checkInDate) { this.checkInDate = checkInDate; }

    public String getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(String checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getStayNights() { return stayNights; }
    public void setStayNights(int stayNights) { this.stayNights = stayNights; }

    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }

    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = discountAmount; }

    public int getTotalPrice() { return totalPrice; }
    public void setTotalPrice(int totalPrice) { this.totalPrice = totalPrice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookedAt() { return bookedAt; }
    public void setBookedAt(String bookedAt) { this.bookedAt = bookedAt; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
