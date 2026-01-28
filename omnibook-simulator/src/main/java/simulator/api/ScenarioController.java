package simulator.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import simulator.scenario.ScenarioResult;
import simulator.scenario.ScenarioRunner;
import simulator.scenario.impl.SimpleBookingScenario;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRunner scenarioRunner;

    public ScenarioController(ScenarioRunner scenarioRunner) {
        this.scenarioRunner = scenarioRunner;
    }

    @PostMapping("/simple-booking")
    public ResponseEntity<ScenarioResponse> runSimpleBooking() {
        ScenarioResult result = scenarioRunner.run(new SimpleBookingScenario());
        ScenarioResponse response = ScenarioResponse.from(result);
        return ResponseEntity.ok(response);
    }
}
