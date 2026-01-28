package simulator.chaos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chaos engine for delivery-level disruption.
 * Never modifies payload content — only affects how/when/how-many-times events are delivered.
 */
public class ChaosEngine {

    private final ChaosConfig config;

    public ChaosEngine(ChaosConfig config) {
        this.config = config;
    }

    /** Decide what chaos to apply to a single event delivery. */
    public ChaosDecision decide() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        boolean dup = rng.nextDouble() < config.getDuplicateProbability();
        int dupCount = dup ? (1 + rng.nextInt(config.getMaxDuplicates())) : 0;

        boolean delay = rng.nextDouble() < config.getDelayProbability();
        long delayMs = delay ? (100 + rng.nextLong(config.getMaxDelayMs())) : 0;

        boolean fail = rng.nextDouble() < config.getFailureProbability();

        return new ChaosDecision(dup, dupCount, delay, delayMs, fail);
    }

    /**
     * Apply reorder chaos to a list of event indices.
     * Returns a potentially shuffled copy of the input list.
     */
    public <T> List<T> maybeReorder(List<T> events) {
        if (ThreadLocalRandom.current().nextDouble() < config.getReorderProbability()) {
            List<T> shuffled = new ArrayList<>(events);
            Collections.shuffle(shuffled);
            return shuffled;
        }
        return events;
    }
}
