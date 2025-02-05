package Its.incom.pw5.rest.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PreDestroy;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.jboss.logging.Logger;
import jakarta.inject.Inject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

@ApplicationScoped
public class MetricsLogger {

    private static final Logger LOG = Logger.getLogger(MetricsLogger.class);

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry metricRegistry;

    @PreDestroy
    public void logMetricsToFile() {
        String filePath = "logs/metrics.log";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("==== Metrics Summary ====\n");

            // Log each counter and timer metric
            metricRegistry.getCounters().forEach((name, counter) -> {
                try {
                    writer.write(String.format("Counter [%s]: %d\n", name, counter.getCount()));
                } catch (IOException e) {
                    LOG.error("Error writing counter metric", e);
                }
            });

            metricRegistry.getTimers().forEach((name, timer) -> {
                try {
                    writer.write(String.format("Timer [%s]: Count = %d, Total Time = %.3f seconds\n",
                            name, timer.getCount(), timer.getSnapshot().getMean() / 1_000_000_000.0));
                } catch (IOException e) {
                    LOG.error("Error writing timer metric", e);
                }
            });

            writer.write("==== End of Metrics ====\n\n");

        } catch (IOException e) {
            LOG.error("Failed to write metrics to file", e);
        }
    }
}
