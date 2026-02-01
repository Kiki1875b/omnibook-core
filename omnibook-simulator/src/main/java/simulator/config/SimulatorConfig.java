package simulator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import simulator.chaos.ChaosConfig;
import simulator.platform.OtaPlatform;
import simulator.platform.PlatformType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SimulatorConfig {

    @Bean
    public ChaosConfig chaosConfig() {
        return ChaosConfig.defaults();
    }

    @Bean
    public Map<PlatformType, OtaPlatform> platforms(List<OtaPlatform> platformList) {
        Map<PlatformType, OtaPlatform> map = new LinkedHashMap<>();
        for (OtaPlatform platform : platformList) {
            map.put(platform.type(), platform);
        }
        return map;
    }
}
