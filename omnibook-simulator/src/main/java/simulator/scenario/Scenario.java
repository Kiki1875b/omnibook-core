package simulator.scenario;

/**
 * A reproducible cross-platform simulation scenario.
 * Each scenario must involve MULTIPLE platforms.
 */
public interface Scenario {

    String name();

    /**
     * Execute the scenario using the provided context.
     * The scenario creates reservations, cancellations, and emits events.
     */
    ScenarioResult execute(ScenarioContext context);
}
