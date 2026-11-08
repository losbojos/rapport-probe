package rapport.probe;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;

public class ProbeScheduler implements ApplicationRunner {
    private final ProbeProperties properties;
    private final HttpTargetChecker checker;
    private final ProbeSnapshot snapshot;
    private final TargetMetrics metrics;

    public ProbeScheduler(
            ProbeProperties properties,
            HttpTargetChecker checker,
            ProbeSnapshot snapshot,
            TargetMetrics metrics
    ) {
        this.properties = properties;
        this.checker = checker;
        this.snapshot = snapshot;
        this.metrics = metrics;
    }

    @Override
    public void run(ApplicationArguments args) {
        poll();
    }

    @Scheduled(fixedDelayString = "${probe.interval}", initialDelayString = "${probe.interval}")
    public void poll() {
        for (ProbeProperties.Target target : properties.targets()) {
            ProbeResult result = checker.check(target);
            snapshot.save(result);
            metrics.record(result);
        }
    }
}
