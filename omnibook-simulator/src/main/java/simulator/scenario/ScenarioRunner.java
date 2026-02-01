package simulator.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import simulator.chaos.ChaosEngine;
import simulator.platform.OtaPlatform;
import simulator.platform.PlatformType;
import simulator.report.ExecutionReport;
import simulator.report.ReportPrinter;
import simulator.sender.EventSender;

import java.util.Map;
import java.util.UUID;

/**
 * Runs a single scenario with a fresh ScenarioContext and unique correlationId.
 */
@Service
@RequiredArgsConstructor
public class ScenarioRunner {

    private final Map<PlatformType, OtaPlatform> platforms;
    private final EventSender sender;
    private final ChaosEngine chaosEngine;
    private final ReportPrinter printer;

    @Value("${simulator.target-url}")
    private String targetUrl;

    public ScenarioResult run(Scenario scenario) {
        String correlationId = UUID.randomUUID().toString();
        ExecutionReport report = new ExecutionReport(scenario.name(), correlationId);

        ScenarioContext context = new ScenarioContext(
                platforms, sender, chaosEngine, targetUrl, correlationId, report);

        System.out.println();
        System.out.printf(">> Running scenario: %s%n", scenario.name());
        System.out.printf("   Correlation: %s%n", correlationId);
        System.out.println("   ----------------------------------------");

        ScenarioResult result = scenario.execute(context);

        printer.print(report);

        if (!result.isSuccess()) {
            System.out.printf("   SCENARIO FAILED: %s%n", result.getErrorMessage());
        }

        return result;
    }
}
