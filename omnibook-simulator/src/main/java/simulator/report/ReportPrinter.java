package simulator.report;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Console-based execution report printer.
 */
@Component
public class ReportPrinter {

    public void print(ExecutionReport report) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf( "║  SCENARIO: %-49s║%n", report.getScenarioName());
        System.out.printf( "║  CORRELATION: %-46s║%n", report.getCorrelationId());
        System.out.printf( "║  DURATION: %-49s║%n", report.getDurationMs() + " ms");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  %-10s %-14s %-14s %-10s %-6s ║%n",
                "PLATFORM", "EVENT", "RESERVATION", "CHAOS", "OK?");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        List<ExecutionReport.Entry> entries = report.getEntries();
        for (ExecutionReport.Entry e : entries) {
            System.out.printf("║  %-10s %-14s %-14s %-10s %-6s ║%n",
                    e.getPlatform().displayName(),
                    e.getEventType(),
                    truncate(e.getReservationId(), 14),
                    truncate(e.getChaosEffect(), 10),
                    e.isDelivered() ? "YES" : "NO");
        }

        System.out.println("╠══════════════════════════════════════════════════════════════╣");

        long total = entries.size();
        long delivered = entries.stream().filter(ExecutionReport.Entry::isDelivered).count();
        long failed = total - delivered;
        long chaotic = entries.stream()
                .filter(e -> !"CLEAN".equals(e.getChaosEffect()))
                .count();

        System.out.printf( "║  TOTAL: %-5d  DELIVERED: %-5d  FAILED: %-5d  CHAOTIC: %-3d ║%n",
                total, delivered, failed, chaotic);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 2) + "..";
    }
}
