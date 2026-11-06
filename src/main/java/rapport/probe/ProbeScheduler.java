package rapport.probe;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;

public class ProbeScheduler implements ApplicationRunner {
    private final ProbeProperties properties;
    private final HttpTargetChecker checker;
    private final ProbeSnapshot snapshot;

    public ProbeScheduler(ProbeProperties properties, HttpTargetChecker checker, ProbeSnapshot snapshot) {
        this.properties = properties;
        this.checker = checker;
        this.snapshot = snapshot;
    }

    @Override
    public void run(ApplicationArguments args) {
        poll();
    }

    @Scheduled(fixedDelayString = "${probe.interval}", initialDelayString = "${probe.interval}")
    public void poll() {
        for (ProbeProperties.Target target : properties.targets()) {
            snapshot.save(checker.check(target));
        }
    }
}
