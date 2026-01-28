package simulator.chaos;

/**
 * Result of the chaos engine evaluating a single event delivery.
 * Describes what chaos effects should be applied.
 */
public class ChaosDecision {

    private final boolean duplicate;
    private final int duplicateCount;
    private final boolean delay;
    private final long delayMs;
    private final boolean fail;

    public ChaosDecision(boolean duplicate, int duplicateCount,
                         boolean delay, long delayMs,
                         boolean fail) {
        this.duplicate = duplicate;
        this.duplicateCount = duplicateCount;
        this.delay = delay;
        this.delayMs = delayMs;
        this.fail = fail;
    }

    public static ChaosDecision clean() {
        return new ChaosDecision(false, 0, false, 0, false);
    }

    public boolean isDuplicate() { return duplicate; }
    public int getDuplicateCount() { return duplicateCount; }
    public boolean isDelay() { return delay; }
    public long getDelayMs() { return delayMs; }
    public boolean isFail() { return fail; }

    @Override
    public String toString() {
        if (!duplicate && !delay && !fail) return "CLEAN";
        StringBuilder sb = new StringBuilder();
        if (duplicate) sb.append("DUP(x").append(duplicateCount).append(") ");
        if (delay) sb.append("DELAY(").append(delayMs).append("ms) ");
        if (fail) sb.append("FAIL ");
        return sb.toString().trim();
    }
}
