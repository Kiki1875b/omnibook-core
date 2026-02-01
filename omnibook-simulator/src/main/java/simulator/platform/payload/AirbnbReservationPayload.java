package simulator.platform.payload;

import lombok.Data;

/**
 * Platform B — AIRBNB (Global host-based OTA).
 */
@Data
public class AirbnbReservationPayload {

    private String confirmationCode;    // ABC123XYZW
    private String listingId;
    private String hostId;

    private String checkIn;             // ISO-8601 date yyyy-MM-dd
    private String checkOut;            // ISO-8601 date yyyy-MM-dd
    private int nights;

    private String guestFirstName;
    private String guestLastName;
    private String guestEmail;
    private int numberOfGuests;
    private int numberOfAdults;
    private int numberOfChildren;
    private int numberOfInfants;

    private double totalPayout;         // host receives
    private double hostServiceFee;
    private double guestServiceFee;
    private double cleaningFee;
    private String currency;            // ISO-4217 e.g. "USD", "KRW"

    private String status;              // ACCEPTED | PENDING | CANCELLED | DENIED
    private String cancellationPolicy;  // FLEXIBLE | MODERATE | STRICT

    private String timezone;            // IANA e.g. "Asia/Seoul", "America/New_York"
    private long createdAt;             // epoch millis
    private long updatedAt;             // epoch millis
}
