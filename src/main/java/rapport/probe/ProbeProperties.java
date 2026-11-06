package rapport.probe;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "probe")
public record ProbeProperties(long interval, List<Target> targets) {
    public ProbeProperties {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }

    public record Target(String name, String url) {}
}
