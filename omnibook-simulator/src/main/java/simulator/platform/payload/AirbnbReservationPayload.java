package simulator.platform.payload;

/**
 * Platform B — AIRBNB (Global host-based OTA).
 *
 * Characteristics:
 * - Nights-based model
 * - Host-oriented fields (hostId, hostPayout)
 * - Split guest name (first/last)
 * - Prices in decimal with currency code
 * - Global timezone field
 * - Epoch millis for timestamps
 * - Strict check-in / check-out semantics
 */
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

    public AirbnbReservationPayload() {}

    // --- getters / setters ---

    public String getConfirmationCode() { return confirmationCode; }
    public void setConfirmationCode(String confirmationCode) { this.confirmationCode = confirmationCode; }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }

    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }

    public int getNights() { return nights; }
    public void setNights(int nights) { this.nights = nights; }

    public String getGuestFirstName() { return guestFirstName; }
    public void setGuestFirstName(String guestFirstName) { this.guestFirstName = guestFirstName; }

    public String getGuestLastName() { return guestLastName; }
    public void setGuestLastName(String guestLastName) { this.guestLastName = guestLastName; }

    public String getGuestEmail() { return guestEmail; }
    public void setGuestEmail(String guestEmail) { this.guestEmail = guestEmail; }

    public int getNumberOfGuests() { return numberOfGuests; }
    public void setNumberOfGuests(int numberOfGuests) { this.numberOfGuests = numberOfGuests; }

    public int getNumberOfAdults() { return numberOfAdults; }
    public void setNumberOfAdults(int numberOfAdults) { this.numberOfAdults = numberOfAdults; }

    public int getNumberOfChildren() { return numberOfChildren; }
    public void setNumberOfChildren(int numberOfChildren) { this.numberOfChildren = numberOfChildren; }

    public int getNumberOfInfants() { return numberOfInfants; }
    public void setNumberOfInfants(int numberOfInfants) { this.numberOfInfants = numberOfInfants; }

    public double getTotalPayout() { return totalPayout; }
    public void setTotalPayout(double totalPayout) { this.totalPayout = totalPayout; }

    public double getHostServiceFee() { return hostServiceFee; }
    public void setHostServiceFee(double hostServiceFee) { this.hostServiceFee = hostServiceFee; }

    public double getGuestServiceFee() { return guestServiceFee; }
    public void setGuestServiceFee(double guestServiceFee) { this.guestServiceFee = guestServiceFee; }

    public double getCleaningFee() { return cleaningFee; }
    public void setCleaningFee(double cleaningFee) { this.cleaningFee = cleaningFee; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(String cancellationPolicy) { this.cancellationPolicy = cancellationPolicy; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
