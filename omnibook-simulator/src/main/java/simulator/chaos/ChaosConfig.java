package simulator.chaos;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Configuration knobs for the chaos engine.
 * Probabilities are 0.0-1.0. Delay is in milliseconds.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChaosConfig {

    private double duplicateProbability;
    private int maxDuplicates;
    private double delayProbability;
    private long maxDelayMs;
    private double reorderProbability;
    private double failureProbability;

    public static ChaosConfig defaults() {
        return new ChaosConfig(0.15, 2, 0.20, 3000, 0.15, 0.10);
    }

    public static ChaosConfig none() {
        return new ChaosConfig(0, 0, 0, 0, 0, 0);
    }
}
