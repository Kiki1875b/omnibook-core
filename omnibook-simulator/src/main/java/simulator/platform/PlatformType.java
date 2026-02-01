package simulator.platform;

/**
 * Fixed platform mapping — do not change.
 *
 * A = YANOLJA   (Korean domestic OTA, mobile-first)
 * B = AIRBNB    (Global host-based OTA)
 * C = YEOGIEOTTAE (Korean domestic OTA, batch-oriented)
 */
public enum PlatformType {

    A("YANOLJA"),
    B("AIRBNB"),
    C("YEOGIEOTTAE");

    private final String displayName;

    PlatformType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
