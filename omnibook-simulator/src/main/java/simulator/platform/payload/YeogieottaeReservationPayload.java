package simulator.platform.payload;

import lombok.Data;

/**
 * Platform C — YEOGIEOTTAE (Korean domestic OTA, batch-oriented).
 */
@Data
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
}
