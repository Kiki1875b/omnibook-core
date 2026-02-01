package simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Omnibook Simulator — cross-platform OTA reservation event emitter.
 *
 * Platform mapping (fixed):
 *   A = YANOLJA       (Korean domestic OTA, mobile-first)
 *   B = AIRBNB        (Global host-based OTA)
 *   C = YEOGIEOTTAE   (Korean domestic OTA, batch-oriented)
 *
 * Scenarios are triggered via REST API endpoints.
 */
@SpringBootApplication
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}
