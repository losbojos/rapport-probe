package rapport.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ProbeSnapshot {
    private final ConcurrentHashMap<String, ProbeResult> latest = new ConcurrentHashMap<>();

    public void save(ProbeResult result) {
        latest.put(result.name(), result);
    }

    public List<ProbeResult> findAll(List<ProbeProperties.Target> targets) {
        List<ProbeResult> results = new ArrayList<>();
        for (ProbeProperties.Target target : targets) {
            ProbeResult result = latest.get(target.name());
            if (result != null) {
                results.add(result);
            }
        }
        return List.copyOf(results); // Неизменяемый список
    }
}
