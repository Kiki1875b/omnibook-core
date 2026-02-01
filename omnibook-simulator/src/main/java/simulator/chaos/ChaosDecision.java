package simulator.chaos;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of the chaos engine evaluating a single event delivery.
 * Describes what chaos effects should be applied.
 */
@Getter
@AllArgsConstructor
public class ChaosDecision {

    private final boolean duplicate;
    private final int duplicateCount;
    private final boolean delay;
    private final long delayMs;
    private final boolean fail;

    public static ChaosDecision clean() {
        return new ChaosDecision(false, 0, false, 0, false);
    }

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
