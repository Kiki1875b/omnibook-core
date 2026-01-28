package simulator.scenario;

import simulator.report.ExecutionReport;

/**
 * Outcome of a single scenario execution.
 */
public class ScenarioResult {

    private final String scenarioName;
    private final boolean success;
    private final ExecutionReport report;
    private final String errorMessage;

    private ScenarioResult(String scenarioName, boolean success,
                           ExecutionReport report, String errorMessage) {
        this.scenarioName = scenarioName;
        this.success = success;
        this.report = report;
        this.errorMessage = errorMessage;
    }

    public static ScenarioResult ok(String name, ExecutionReport report) {
        return new ScenarioResult(name, true, report, null);
    }

    public static ScenarioResult fail(String name, ExecutionReport report, String error) {
        return new ScenarioResult(name, false, report, error);
    }

    public String getScenarioName() { return scenarioName; }
    public boolean isSuccess() { return success; }
    public ExecutionReport getReport() { return report; }
    public String getErrorMessage() { return errorMessage; }
}
