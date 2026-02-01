package simulator.scenario;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import simulator.report.ExecutionReport;

/**
 * Outcome of a single scenario execution.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScenarioResult {

    private final String scenarioName;
    private final boolean success;
    private final ExecutionReport report;
    private final String errorMessage;

    public static ScenarioResult ok(String name, ExecutionReport report) {
        return new ScenarioResult(name, true, report, null);
    }

    public static ScenarioResult fail(String name, ExecutionReport report, String error) {
        return new ScenarioResult(name, false, report, error);
    }
}
