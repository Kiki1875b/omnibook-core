package simulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import simulator.chaos.ChaosConfig;
import simulator.chaos.ChaosEngine;
import simulator.platform.AirbnbPlatform;
import simulator.platform.OtaPlatform;
import simulator.platform.PlatformType;
import simulator.platform.YanoljaPlatform;
import simulator.platform.YeogieottaePlatform;
import simulator.scenario.ScenarioRunner;
import simulator.sender.EventSender;
import simulator.sender.HttpEventSender;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class SimulatorConfig {

    @Value("${simulator.target-url}")
    private String targetUrl;

    @Bean
    public Map<PlatformType, OtaPlatform> platforms() {
        Map<PlatformType, OtaPlatform> map = new LinkedHashMap<>();
        map.put(PlatformType.A, new YanoljaPlatform());
        map.put(PlatformType.B, new AirbnbPlatform());
        map.put(PlatformType.C, new YeogieottaePlatform());
        return map;
    }

    @Bean
    public ChaosEngine chaosEngine() {
        return new ChaosEngine(ChaosConfig.defaults());
    }

    @Bean
    public EventSender eventSender() {
        return new HttpEventSender();
    }

    @Bean
    public ScenarioRunner scenarioRunner(Map<PlatformType, OtaPlatform> platforms,
                                         EventSender eventSender,
                                         ChaosEngine chaosEngine) {
        return new ScenarioRunner(platforms, eventSender, chaosEngine, targetUrl);
    }
}
