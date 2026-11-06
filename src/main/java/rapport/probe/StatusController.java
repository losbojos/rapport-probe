package rapport.probe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StatusController {
    private final ProbeProperties properties;
    private final ProbeSnapshot snapshot;

    public StatusController(ProbeProperties properties, ProbeSnapshot snapshot) {
        this.properties = properties;
        this.snapshot = snapshot;
    }

    @GetMapping("/status")
    public List<ProbeResult> status() {
        return snapshot.findAll(properties.targets());
    }
}
