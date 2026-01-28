package simulator.chaos;

/**
 * Configuration knobs for the chaos engine.
 * Probabilities are 0.0–1.0. Delay is in milliseconds.
 */
public class ChaosConfig {

    private double duplicateProbability;
    private int maxDuplicates;         // how many extra copies (1–3)
    private double delayProbability;
    private long maxDelayMs;
    private double reorderProbability;
    private double failureProbability;

    private ChaosConfig() {}

    public static ChaosConfig defaults() {
        ChaosConfig c = new ChaosConfig();
        c.duplicateProbability = 0.15;
        c.maxDuplicates = 2;
        c.delayProbability = 0.20;
        c.maxDelayMs = 3000;
        c.reorderProbability = 0.15;
        c.failureProbability = 0.10;
        return c;
    }

    public static ChaosConfig none() {
        ChaosConfig c = new ChaosConfig();
        c.duplicateProbability = 0;
        c.maxDuplicates = 0;
        c.delayProbability = 0;
        c.maxDelayMs = 0;
        c.reorderProbability = 0;
        c.failureProbability = 0;
        return c;
    }

    public double getDuplicateProbability() { return duplicateProbability; }
    public int getMaxDuplicates() { return maxDuplicates; }
    public double getDelayProbability() { return delayProbability; }
    public long getMaxDelayMs() { return maxDelayMs; }
    public double getReorderProbability() { return reorderProbability; }
    public double getFailureProbability() { return failureProbability; }
}
