package simulator.platform.payload;

/**
 * Platform C — YEOGIEOTTAE (Korean domestic OTA, batch-oriented).
 *
 * Characteristics:
 * - Compact date format (yyyyMMdd — no dashes)
 * - Coarse integer state codes
 * - Epoch seconds (NOT millis — intentional divergence from Airbnb)
 * - Buyer terminology instead of guest
 * - Batch-oriented delivery: delayed or reordered events are normal
 * - No coupon fields (unlike Yanolja)
 */
public class YeogieottaeReservationPayload {

    private String orderId;             // YEO-xxxxxxxx
    private String accommodationId;
    private String roomTypeId;
    private String roomTypeName;

    private String startDate;           // yyyyMMdd (compact, no dashes)
    private String endDate;             // yyyyMMdd (compact, no dashes)

    private String buyerName;
    private String buyerTel;            // 01012345678 (no dashes)

    private int totalAmount;            // KRW integer
    private String payMethod;           // CARD | PHONE | BANK_TRANSFER

    private int state;                  // 1=BOOKED, 2=CANCELLED, 3=COMPLETED, 4=NOSHOW
    private long registeredTs;          // epoch SECONDS (not millis)
    private long lastModifiedTs;        // epoch SECONDS

    public YeogieottaeReservationPayload() {}

    // --- getters / setters ---

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getAccommodationId() { return accommodationId; }
    public void setAccommodationId(String accommodationId) { this.accommodationId = accommodationId; }

    public String getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(String roomTypeId) { this.roomTypeId = roomTypeId; }

    public String getRoomTypeName() { return roomTypeName; }
    public void setRoomTypeName(String roomTypeName) { this.roomTypeName = roomTypeName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }

    public String getBuyerTel() { return buyerTel; }
    public void setBuyerTel(String buyerTel) { this.buyerTel = buyerTel; }

    public int getTotalAmount() { return totalAmount; }
    public void setTotalAmount(int totalAmount) { this.totalAmount = totalAmount; }

    public String getPayMethod() { return payMethod; }
    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }

    public int getState() { return state; }
    public void setState(int state) { this.state = state; }

    public long getRegisteredTs() { return registeredTs; }
    public void setRegisteredTs(long registeredTs) { this.registeredTs = registeredTs; }

    public long getLastModifiedTs() { return lastModifiedTs; }
    public void setLastModifiedTs(long lastModifiedTs) { this.lastModifiedTs = lastModifiedTs; }
}
