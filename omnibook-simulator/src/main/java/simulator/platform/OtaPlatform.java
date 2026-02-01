package simulator.platform;

/**
 * Contract for an OTA platform simulator.
 * Each platform emits events using its own payload shape.
 * No shared payload structure — intentional.
 */
public interface OtaPlatform {

    PlatformType type();

    /** Create a booking and return the platform-specific payload. */
    Object book(String roomId, String guestName, String checkIn, String checkOut);

    /** Cancel an existing booking and return the platform-specific cancel payload. */
    Object cancel(String reservationId);

    /** Check whether a reservation exists in this platform's internal state. */
    boolean hasReservation(String reservationId);
}
